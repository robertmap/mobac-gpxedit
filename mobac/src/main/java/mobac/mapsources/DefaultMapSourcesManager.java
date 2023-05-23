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

import mobac.mapsources.custom.StandardMapSourceLayer;
import mobac.mapsources.impl.DebugLocalMapSource;
import mobac.mapsources.impl.DebugMapSource;
import mobac.mapsources.impl.DebugRandomLocalMapSource;
import mobac.mapsources.impl.DebugTransparentLocalMapSource;
import mobac.mapsources.impl.SimpleMapSource;
import mobac.mapsources.loader.BeanShellMapSourceLoader;
import mobac.mapsources.loader.CustomMapSourceLoader;
import mobac.mapsources.loader.IntelliJMapPackLoader;
import mobac.mapsources.loader.MapPackManager;
import mobac.program.interfaces.MapSource;
import mobac.program.model.Settings;
import mobac.utilities.I18nUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.Vector;

public class DefaultMapSourcesManager extends MapSourcesManager {

	private final Logger log = LoggerFactory.getLogger(DefaultMapSourcesManager.class);

	/**
	 * All map sources visible to the user independent of it is enabled or disabled
	 */
	private final LinkedHashMap<String, MapSource> allMapSources = new LinkedHashMap<>(50);

	/**
	 * All means all visible map sources to the user plus all layers of multi-layer
	 * map sources
	 */
	private final HashMap<String, MapSource> allAvailableMapSources = new HashMap<>(50);

	public DefaultMapSourcesManager() {
		// Check for user specific configuration of mapsources directory
	}

	public static void initialize() {
		DefaultMapSourcesManager manager = new DefaultMapSourcesManager();
		INSTANCE = manager;
		manager.loadMapSources();
	}

	public static void initializeIntelliJMapPacksOnly() {
		DefaultMapSourcesManager manager = new DefaultMapSourcesManager();
		INSTANCE = manager;
		manager.loadMapPacksIntelliJMode();
	}

	protected void loadMapSources() {
		try {
			boolean devMode = Settings.getInstance().devMode;
			addMapSource(new MapsforgeMapSource());
			if (devMode) {
				addMapSource(new DebugMapSource());
				addMapSource(new DebugLocalMapSource());
				addMapSource(new DebugTransparentLocalMapSource());
				addMapSource(new DebugRandomLocalMapSource());
			}
			File mapSourcesDir = Settings.getInstance().getMapSourcesDirectory();
			if (mapSourcesDir == null) {
				throw new RuntimeException("Map sources directory is not set");
			}
			if (!mapSourcesDir.isDirectory()) {
				JOptionPane.showMessageDialog(null,
						String.format(I18nUtils.localizedStringForKey("msg_environment_mapsrc_dir_not_exist"),
								mapSourcesDir.getAbsolutePath()),
						I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			try {
				MapPackManager mpm = new MapPackManager(mapSourcesDir);
				mpm.installUpdates();
				if (!loadMapPacksIntelliJMode()) {
					mpm.loadMapPacks(this);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to load map packs: " + e.getMessage(), e);
			}

			// Always load custom BSH map sources first. This allows to use them in custom
			// multi-layer map sources.
			BeanShellMapSourceLoader bsmsl = new BeanShellMapSourceLoader(this, mapSourcesDir);
			bsmsl.loadBeanShellMapSources();

			CustomMapSourceLoader cmsl = new CustomMapSourceLoader(this, mapSourcesDir);
			cmsl.loadCustomMapSources();

		} finally {
			// If no map sources are available load the simple map source which shows the
			// informative message
			if (allMapSources.isEmpty()) {
				addMapSource(new SimpleMapSource());
			}
		}
	}

	private boolean loadMapPacksIntelliJMode() {
		IntelliJMapPackLoader empl;
		try {
			empl = new IntelliJMapPackLoader(this);
			return empl.loadMapPacks();
		} catch (IOException e) {
			log.error("Failed to load map packs directly from classpath");
		}
		return false;
	}

	public void addMapSource(MapSource mapSource) {
		if (mapSource instanceof StandardMapSourceLayer) {
			mapSource = ((StandardMapSourceLayer) mapSource).getMapSource();
		}
		allAvailableMapSources.put(mapSource.getName(), mapSource);
		if (mapSource instanceof AbstractMultiLayerMapSource) {
			for (MapSource multiLayerMapSource : ((AbstractMultiLayerMapSource) mapSource)) {
				if (multiLayerMapSource instanceof StandardMapSourceLayer) {
					multiLayerMapSource = ((StandardMapSourceLayer) multiLayerMapSource).getMapSource();
				}
				MapSource old = allAvailableMapSources.put(multiLayerMapSource.getName(), multiLayerMapSource);
				if (old != null) {
					allAvailableMapSources.put(old.getName(), old);
					if (mapSource.equals(old)) {
						JOptionPane.showMessageDialog(null,
								"Error: Duplicate map source name found: " + mapSource.getName(), "Duplicate name",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}

		String mapSourceName = mapSource.getName();
		if (allMapSources.containsKey(mapSourceName)) {
			JOptionPane.showMessageDialog(null,
					String.format(I18nUtils.localizedStringForKey("msg_environment_error_duplicate_map_source"),
							mapSourceName, mapSource.getLoaderInfo().getSourceFile()),
					I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
		} else {
			allMapSources.put(mapSourceName, mapSource);
		}
	}

	@Override
	public Vector<MapSource> getAllAvailableMapSources() {
		return new Vector<MapSource>(allMapSources.values());
	}

	@Override
	public Vector<MapSource> getAllMapSources() {
		return new Vector<MapSource>(allMapSources.values());
	}

	@Override
	public Vector<MapSource> getAllLayerMapSources() {
		Vector<MapSource> all = getAllMapSources();
		TreeSet<MapSource> uniqueSources = new TreeSet<>((o1, o2) -> o1.getName().compareTo(o2.getName()));
		for (MapSource ms : all) {
			if (ms instanceof AbstractMultiLayerMapSource) {
				for (MapSource lms : ((AbstractMultiLayerMapSource) ms)) {
					uniqueSources.add(lms);
				}
			} else {
				uniqueSources.add(ms);
			}
		}
		Vector<MapSource> result = new Vector<>(uniqueSources);
		return result;
	}

	@Override
	public Vector<MapSource> getEnabledOrderedMapSources() {
		Vector<MapSource> mapSources = new Vector<>(allMapSources.size());

		Vector<String> enabledMapSources = Settings.getInstance().mapSourcesEnabled;
		TreeSet<String> notEnabledMapSources = new TreeSet<>(allMapSources.keySet());
		notEnabledMapSources.removeAll(enabledMapSources);
		for (String mapSourceName : enabledMapSources) {
			MapSource ms = getSourceByName(mapSourceName);
			if (ms != null) {
				mapSources.add(ms);
			}
		}
		// remove all disabled map sources, so we get those that are neither enabled nor
		// disabled
		notEnabledMapSources.removeAll(Settings.getInstance().mapSourcesDisabled);
		for (String mapSourceName : notEnabledMapSources) {
			MapSource ms = getSourceByName(mapSourceName);
			if (ms != null) {
				mapSources.add(ms);
			}
		}
		if (mapSources.isEmpty()) {
			mapSources.add(new SimpleMapSource());
		}
		return mapSources;
	}

	@Override
	public Vector<MapSource> getDisabledMapSources() {
		Vector<String> disabledMapSources = Settings.getInstance().mapSourcesDisabled;
		Vector<MapSource> mapSources = new Vector<>(disabledMapSources.size());
		for (String mapSourceName : disabledMapSources) {
			MapSource ms = getSourceByName(mapSourceName);
			if (ms != null) {
				mapSources.add(ms);
			}
		}
		return mapSources;
	}

	@Override
	public MapSource getDefaultMapSource() {
		MapSource ms = getSourceByName("4uMaps");// DEFAULT;
		if (ms != null) {
			return ms;
		}
		// Fallback: return first
		return allMapSources.values().iterator().next();
	}

	@Override
	public MapSource getSourceByName(String name) {
		return allAvailableMapSources.get(name);
	}

}
