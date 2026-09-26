package com.jeidietfilter.mixin;

import com.jeidietfilter.UneatenConfig;
import com.jeidietfilter.UneatenPrefix;
import mezz.jei.core.search.PrefixInfo;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.search.ElementPrefixParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = ElementPrefixParser.class, remap = false)
public abstract class ElementPrefixParserMixin {

	private static final Logger LOGGER = LogManager.getLogger();

	@Shadow
	private void addPrefix(PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo) {
	}

	// This constructor runs inside IngredientFilter's constructor, before ElementSearch
	// is built, so registering our prefix here is safe.
	@Inject(method = "<init>", at = @At("RETURN"), remap = false)
	private void jeidietfilter$registerUneatenPrefix(CallbackInfo ci) {
		char prefixChar = UneatenConfig.getPrefixChar();
		PrefixInfo<IListElementInfo<?>, IListElement<?>> prefix = UneatenPrefix.create(prefixChar);
		addPrefix(prefix);
		UneatenPrefix.CURRENT = prefix;
		LOGGER.info("[JEIDietFilter] Registered uneaten-food prefix '{}'", prefixChar);
	}

	// JEI's native parseToken returns Optional.empty() for a bare 1-char prefix token,
	// so intercept at HEAD: a bare prefix matches every uneaten food via the "uneaten"
	// sentinel string, and "<prefix><text>" searches uneaten foods by text.
	// 全角"！"（中文输入法）也一并支持（仅当配置的前缀是半角"!"时）。
	@Inject(method = "parseToken", at = @At("HEAD"), cancellable = true, remap = false)
	private void jeidietfilter$parseBarePrefix(String token, CallbackInfoReturnable<Optional<ElementPrefixParser.TokenInfo>> cir) {
		String query = matchUneatenQuery(token, UneatenConfig.getPrefixChar());
		if (query != null) {
			cir.setReturnValue(Optional.of(new ElementPrefixParser.TokenInfo(query, UneatenPrefix.CURRENT)));
		}
	}

	private static String matchUneatenQuery(String token, char c) {
		String cs = String.valueOf(c);
		if (token.equals(cs)) {
			return "uneaten";
		}
		if (token.startsWith(cs)) {
			String rest = token.substring(1);
			return rest.isEmpty() ? "uneaten" : rest;
		}
		if (c == '!') {
			if (token.equals("！")) {
				return "uneaten";
			}
			if (token.startsWith("！")) {
				String rest = token.substring(1);
				return rest.isEmpty() ? "uneaten" : rest;
			}
		}
		return null;
	}
}
