package com.jeidietfilter;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class DietJeiPlugin implements IModPlugin {

	public static volatile IJeiRuntime RUNTIME;

	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation("jei_diet_filter", "plugin");
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		RUNTIME = runtime;
		DietJeiFilterMod.onJeiRuntimeReady();
	}
}
