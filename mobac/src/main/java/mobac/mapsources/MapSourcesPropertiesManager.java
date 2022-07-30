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
package mobac.mapsources;

import mobac.program.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class MapSourcesPropertiesManager {

	public static final Properties PROPERTIES = new Properties();
	private static final Logger log = LoggerFactory.getLogger(MapSourcesPropertiesManager.class);
	private static final String FILENAME = "mapsources.properties";
	private static boolean SHUTDOWN_HOOK_REGISTERED = false;

	public static void load() {
		File mapSourcesDir = Settings.getInstance().getMapSourcesDirectory();
		File mapSourcesProperties = new File(mapSourcesDir, FILENAME);
		if (!mapSourcesProperties.isFile()) {
			return;
		}
		try (FileInputStream in = new FileInputStream(mapSourcesProperties)) {
			PROPERTIES.load(in);
		} catch (IOException e) {
			log.error("Failed to load mapsources.properties", e);
		}
		if (!SHUTDOWN_HOOK_REGISTERED) {
			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					save();
				}

			});
		}
	}

	public static void save() {
		if (PROPERTIES.size() == 0) {
			return;
		}
		File mapSourcesDir = Settings.getInstance().getMapSourcesDirectory();
		File mapSourcesProperties = new File(mapSourcesDir, FILENAME);
		try (FileOutputStream out = new FileOutputStream(mapSourcesProperties)) {
			PROPERTIES.store(out, "");
		} catch (IOException e) {
			log.error("", e);
		}
	}
}
