package mobac.program.model;

import java.util.Locale;

public enum SupportedLocale {
	SupportLocaleEn(Locale.forLanguageTag("en"), "English"), // default
	SupportLocaleFrFR(Locale.forLanguageTag("fr-FR"), "Français"), // French
	SupportLocaleRuRu(Locale.forLanguageTag("ru-RU"), "Russian"), // Russian
	SupportLocaleJaJP(Locale.forLanguageTag("ja-JP"), "日本語"), // Japanese
	SupportLocaleZhCN(Locale.forLanguageTag("zh-CN"), "简体中文"), // Chinese (simplified)
	SupportLocaleZhTW(Locale.forLanguageTag("zh-TW"), "繁體中文"); // Chinese (Taiwan)

	private final Locale locale;
	private final String displayName;

	SupportedLocale(Locale locale, String displayName) {
		this.locale = locale;
		this.displayName = displayName;
	}

	public static SupportedLocale localeOf(String lang, String country) {
		for (SupportedLocale l : SupportedLocale.values()) {
			if (l.locale.getLanguage().equals(lang) && l.locale.getCountry().equals(country)) {
				return l;
			}
		}
		return SupportLocaleEn;
	}

	@Override
	public String toString() {
		return displayName;
	}

	public Locale getLocale() {
		return locale;
	}

	public String getDisplayName() {
		return displayName;
	}
}
