package com.jeidietfilter;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod("jei_diet_filter")
public class DietJeiFilterMod {

	private static final Logger LOGGER = LogManager.getLogger();

	private static Set<Item> lastEaten = Set.of();
	private static boolean pendingReindex = false;

	public DietJeiFilterMod() {
		if (FMLEnvironment.dist != Dist.CLIENT) {
			return;
		}
		MinecraftForge.EVENT_BUS.addListener(DietJeiFilterMod::onClientTick);
		LOGGER.info("[JEIDietFilter] Registered client tick listener");
	}

	/** 由 DietJeiPlugin.onRuntimeAvailable 调用：JEI 运行时就绪后补做缓存失效 */
	public static void onJeiRuntimeReady() {
		if (!lastEaten.isEmpty()) {
			reindex(lastEaten);
		}
		pendingReindex = false;
	}

	private static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (Minecraft.getInstance().player == null) {
			lastEaten = Set.of();
			return;
		}
		Set<Item> eaten = UneatenFoods.currentEaten();
		if (eaten.size() != lastEaten.size() || !eaten.equals(lastEaten)) {
			Set<Item> newly = new HashSet<>(eaten);
			newly.removeAll(lastEaten);
			lastEaten = Set.copyOf(eaten);
			if (!newly.isEmpty()) {
				if (DietJeiPlugin.RUNTIME != null) {
					reindex(newly);
				} else {
					// JEI 运行时尚未启动，等 onJeiRuntimeReady 补做
					pendingReindex = true;
				}
			}
		}
		if (pendingReindex && DietJeiPlugin.RUNTIME != null) {
			pendingReindex = false;
			reindex(lastEaten);
		}
	}

	private static void reindex(Set<Item> items) {
		List<ItemStack> stacks = items.stream()
			.map(ItemStack::new)
			.toList();
		IIngredientManager ingredientManager = DietJeiPlugin.RUNTIME.getIngredientManager();
		// 同 tick 内 remove + add：强制 JEI 使这些食物的结果缓存失效（吃掉后无需改动搜索词即消失）。
		// 实际的"已吃"剔除在查询时完成，见 mixin.ElementSearchMixin。
		ingredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
		ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
		LOGGER.info("[JEIDietFilter] Invalidated JEI cache for {} eaten food(s)", stacks.size());
	}
}
