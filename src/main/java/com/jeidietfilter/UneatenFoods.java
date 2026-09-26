package com.jeidietfilter;

import com.cazsius.solcarrot.tracking.FoodInstance;
import com.cazsius.solcarrot.tracking.FoodList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据源：solcarrot (Spice of Life: Carrot Edition)。
 * 整合包中物品 tooltip 的"已食用"标记由 solcarrot 渲染
 * （TooltipHandler -> FoodList.get(player).hasEaten(item)），
 * 因此过滤必须走同一条数据路径。Diet 的 eaten 集合是独立记录，两者不一致。
 */
public final class UneatenFoods {

	private UneatenFoods() {
	}

	public static boolean isUneaten(ItemStack stack) {
		if (!stack.isEdible()) {
			return false;
		}
		FoodList foodList = getFoodList();
		if (foodList == null) {
			return true;
		}
		return !foodList.hasEaten(stack.getItem());
	}

	public static Set<Item> currentEaten() {
		FoodList foodList = getFoodList();
		if (foodList == null) {
			return Set.of();
		}
		// Note: getEatenFoods() returns the tracker's live, mutable set.
		// Callers that keep it across ticks must copy it first.
		return foodList.getEatenFoods().stream()
			.map(FoodInstance::getItem)
			.collect(Collectors.toUnmodifiableSet());
	}

	private static FoodList getFoodList() {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return null;
		}
		try {
			// FoodList.get 抛 FoodListNotFoundException 当 capability 未附加时
			return FoodList.get(player);
		} catch (Exception e) {
			return null;
		}
	}
}
