/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.utilities;

import mobac.StartMOBAC;
import mobac.program.model.Settings;
import mobac.utilities.stream.UnicodeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

public class I18nUtils {

	private static final Logger LOG = LoggerFactory.getLogger(I18nUtils.class);

	private static final Locale FALLBACK_LOCALE = Locale.US;

	// MP: return application's resource strings
	private static MyResourceBundle localizationBundle = null;

	private static MyResourceBundle localizationFallbackBundle = null;

	public static String localizedStringForKey(String key, Object... args) {
		if (localizationBundle == null) {
			I18nUtils.updateLocalizedStringFromSettings();
		}
		String str = null;
		try {
			str = localizationBundle.getString(key);
			if (args.length > 0) {
				str = String.format(str, args);
			}
		} catch (MissingResourceException e) {
			LOG.error("Missing localization - for {}", key);
			return key;
		} catch (Exception e) {
			LOG.error("Unexpected error while loading key {}", key, e);
			return key;
		}
		if (str == null) {
			// always return a valid string
			return "";
		}
		return str;
	}

	public static synchronized void updateLocalizedStringFromSettings() {
		Settings settings = Settings.getInstance();
		Locale locale = null;
		if (settings != null) {
			locale = new Locale(settings.localeLanguage, settings.localeCountry);
		} else {
			locale = Locale.getDefault();
		}
		localizationBundle = (MyResourceBundle) ResourceBundle.getBundle("mobac.resources.text.localize", locale,
				new UTF8Control());
		if (!FALLBACK_LOCALE.equals(locale)) {
			// Not sure why we have to load and set the fallback resource manually, but this
			// way it works...
			if (localizationFallbackBundle == null) {
				localizationFallbackBundle = (MyResourceBundle) ResourceBundle
						.getBundle("mobac.resources.text.localize", FALLBACK_LOCALE, new UTF8Control());
			}
			// Check if the current bundle is the fallback bundle. Only set parent if this
			// is not the fallback bundle,
			// otherwise we end up in en endless recursion if a resource string is missing
			if (localizationBundle != localizationFallbackBundle) {
				localizationBundle.setParent(localizationFallbackBundle);
			}
		}
	}

	public static InputStream getI18nResourceAsStream(String name, String extension) {

		Settings s = Settings.getInstance();
		String country = s.localeCountry;
		String language = s.localeLanguage;
		InputStream in;
		in = StartMOBAC.class.getResourceAsStream(String.format("%s_%s_%s.%s", name, language, country, extension));
		if (in != null) {
			return in;
		}
		in = StartMOBAC.class.getResourceAsStream(String.format("%s_%s.%s", name, language, extension));
		if (in != null) {
			return in;
		}
		in = StartMOBAC.class.getResourceAsStream(String.format("%s.%s", name, extension));
		return in;
	}

	/**
	 * http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
	 */
	public static class UTF8Control extends Control {

		public MyResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
				boolean reload) throws IOException {
			// The below is a copy of the default implementation.
			String bundleName = toBundleName(baseName, locale);
			String resourceName = toResourceName(bundleName, "properties");
			InputStream stream = null;
			if (reload) {
				URL url = loader.getResource(resourceName);
				if (url != null) {
					URLConnection connection = url.openConnection();
					if (connection != null) {
						connection.setUseCaches(false);
						stream = connection.getInputStream();
					}
				}
			} else {
				stream = loader.getResourceAsStream(resourceName);
			}
			if (stream != null) {
				try {
					return new MyResourceBundle(new UnicodeReader(stream, StandardCharsets.UTF_8));
				} finally {
					stream.close();
				}
			}
			return null;
		}

		@Override
		public Locale getFallbackLocale(String baseName, Locale locale) {
			return FALLBACK_LOCALE;
		}

	}

	/**
	 * A {@link PropertyResourceBundle} that allows to set the parent property
	 */
	private static class MyResourceBundle extends PropertyResourceBundle {

		public MyResourceBundle(Reader reader) throws IOException {
			super(reader);
		}

		@Override
		public void setParent(ResourceBundle parent) {
			super.setParent(parent);
		}
	}
}
