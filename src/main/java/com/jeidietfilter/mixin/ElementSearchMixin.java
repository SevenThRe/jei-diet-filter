package com.jeidietfilter.mixin;

import com.jeidietfilter.UneatenFoods;
import com.jeidietfilter.UneatenPrefix;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.search.ElementPrefixParser;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * 对本模组前缀的搜索结果做"查询时"过滤：实时剔除已食用食物。
 * 这样无论索引何时构建、Diet 何时同步，结果永远正确。
 * 两个实现类方法签名一致，共用一个 mixin。
 */
@Mixin(targets = {"mezz.jei.gui.search.ElementSearch", "mezz.jei.gui.search.ElementSearchLowMem"}, remap = false)
public abstract class ElementSearchMixin {

	private static final Logger LOGGER = LogManager.getLogger();
	private static int debugCalls = 0;
	private static int probeCalls = 0;

	@Inject(method = "getSearchResults(Lmezz/jei/gui/search/ElementPrefixParser$TokenInfo;)Ljava/util/Set;",
			at = @At("RETURN"), cancellable = true, remap = false)
	private void jeidietfilter$filterEaten(ElementPrefixParser.TokenInfo tokenInfo,
										   CallbackInfoReturnable<Set<IListElement<?>>> cir) {
		if (tokenInfo.prefixInfo() != UneatenPrefix.CURRENT) {
			return;
		}
		Set<IListElement<?>> results = cir.getReturnValue();
		if (results == null || results.isEmpty()) {
			if (debugCalls < 5) {
				debugCalls++;
				LOGGER.info("[JEIDietFilter] filterEaten: token='{}' incoming=0 (empty result)", tokenInfo.token());
			}
			return;
		}
		Set<IListElement<?>> filtered = new HashSet<>(results.size());
		int eatenCount = 0;
		for (IListElement<?> element : results) {
			Object ingredient = element.getTypedIngredient().getIngredient();
			if (ingredient instanceof ItemStack stack && !UneatenFoods.isUneaten(stack)) {
				eatenCount++;
				continue; // 已食用 → 从 ! 结果中剔除
			}
			filtered.add(element);
		}
		if (debugCalls < 10) {
			debugCalls++;
			LOGGER.info("[JEIDietFilter] filterEaten: token='{}' incoming={} eatenRemoved={} kept={}",
					tokenInfo.token(), results.size(), eatenCount, filtered.size());
		}
		cir.setReturnValue(filtered);
	}
}
