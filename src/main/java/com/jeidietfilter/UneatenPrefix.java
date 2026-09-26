package com.jeidietfilter;

import mezz.jei.core.search.LimitedStringStorage;
import mezz.jei.core.search.PrefixInfo;
import mezz.jei.core.search.SearchMode;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class UneatenPrefix {

	/**
	 * The prefix registered into the CURRENTLY live ElementPrefixParser. Re-created whenever
	 * JEI rebuilds its ingredient filter, using the character configured in jei-client.ini.
	 */
	public static volatile PrefixInfo<IListElementInfo<?>, IListElement<?>> CURRENT = create('!');

	private UneatenPrefix() {
	}

	public static PrefixInfo<IListElementInfo<?>, IListElement<?>> create(char prefixChar) {
		return new PrefixInfo<>(
			prefixChar,
			() -> SearchMode.REQUIRE_PREFIX,
			UneatenPrefix::getStrings,
			LimitedStringStorage::new
		);
	}

	// Called by JEI's element indexing thread (via our mixin). Must be fast and side-effect free.
	// 索引阶段只判定"可食用"（与玩家状态无关，避免 Diet 同步时序导致索引过期）；
	// "是否已食用"在查询时过滤，见 mixin.ElementSearchMixin
	private static Collection<String> getStrings(IListElementInfo<?> info) {
		Object ingredient = info.getTypedIngredient().getIngredient();
		if (!(ingredient instanceof ItemStack stack) || !stack.isEdible()) {
			return List.of();
		}
		List<String> strings = new ArrayList<>(info.getNames());
		strings.add("uneaten");
		return strings;
	}
}
