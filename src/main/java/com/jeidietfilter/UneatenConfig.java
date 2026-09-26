package com.jeidietfilter;

import mezz.jei.api.runtime.config.IJeiConfigValueSerializer;
import mezz.jei.common.config.file.ConfigValue;
import mezz.jei.common.config.file.IConfigCategoryBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Registers a "UneatenFoodPrefix" string option into JEI's [search] section of jei-client.ini,
 * letting the user customize the search prefix character for the uneaten-food filter.
 */
public final class UneatenConfig {

	public static final String OPTION_NAME = "UneatenFoodPrefix";
	public static final String DEFAULT_PREFIX = "!";

	private static volatile ConfigValue<String> prefixOption;

	private UneatenConfig() {
	}

	/**
	 * Called by ConfigSchemaBuilderMixin right before JEI builds the jei-client.ini schema,
	 * with the same category builder JEI uses for [search]. Must be idempotent per schema.
	 */
	public static void register(IConfigCategoryBuilder searchCategory) {
		if (prefixOption != null) {
			return;
		}
		ConfigValue<String> value = new ConfigValue<>(
			OPTION_NAME,
			DEFAULT_PREFIX,
			StringSerializer.INSTANCE,
			"Prefix character for the uneaten-food search of JEI Diet Filter (first char is used). " +
				"Example: '!' lets you search with '!'. Requires a game restart to take effect."
		);
		prefixOption = ((mezz.jei.common.config.file.ConfigCategoryBuilder) searchCategory).addValue(value);
	}

	public static char getPrefixChar() {
		ConfigValue<String> option = prefixOption;
		if (option != null) {
			String s = option.get();
			if (s != null && !s.isEmpty()) {
				return s.charAt(0);
			}
		}
		return DEFAULT_PREFIX.charAt(0);
	}

	/** JEI's config system has no built-in string serializer, so provide one. */
	public static final class StringSerializer implements IJeiConfigValueSerializer<String> {
		public static final StringSerializer INSTANCE = new StringSerializer();

		private StringSerializer() {
		}

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			return new Result(string);
		}

		@Override
		public boolean isValid(String value) {
			return value != null && !value.isEmpty();
		}

		@Override
		public Optional<java.util.Collection<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "Any non-empty string; the first character is used as the search prefix";
		}

		private record Result(String value) implements IDeserializeResult<String> {
			@Override
			public Optional<String> getResult() {
				return Optional.ofNullable(value);
			}

			@Override
			public List<String> getErrors() {
				return List.of();
			}
		}
	}
}
