package com.jeidietfilter.mixin;

import com.jeidietfilter.UneatenConfig;
import mezz.jei.common.config.file.ConfigSchemaBuilder;
import mezz.jei.common.config.file.IConfigCategoryBuilder;
import mezz.jei.common.config.file.IConfigSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks JEI's config schema builder: when JEI creates the [search] category of jei-client.ini
 * (from IngredientFilterConfig's constructor), we capture the returned category builder and
 * inject our "UneatenFoodPrefix" option right before the schema is built.
 */
@Mixin(value = ConfigSchemaBuilder.class, remap = false)
public abstract class ConfigSchemaBuilderMixin {

	private static final Logger LOGGER = LogManager.getLogger();

	@Unique
	private IConfigCategoryBuilder jeidietfilter$searchCategory;

	@Inject(method = "addCategory", at = @At("RETURN"), remap = false)
	private void jeidietfilter$captureSearchCategory(String name, CallbackInfoReturnable<IConfigCategoryBuilder> cir) {
		if ("search".equals(name)) {
			this.jeidietfilter$searchCategory = cir.getReturnValue();
		}
	}

	@Inject(method = "build", at = @At("HEAD"), remap = false)
	private void jeidietfilter$registerOption(CallbackInfoReturnable<IConfigSchema> cir) {
		IConfigCategoryBuilder searchCategory = this.jeidietfilter$searchCategory;
		if (searchCategory == null) {
			return;
		}
		this.jeidietfilter$searchCategory = null;
		try {
			UneatenConfig.register(searchCategory);
			LOGGER.info("[JEIDietFilter] Registered {} option into jei-client.ini [search]", UneatenConfig.OPTION_NAME);
		} catch (Throwable t) {
			LOGGER.error("[JEIDietFilter] Failed to register config option", t);
		}
	}
}
