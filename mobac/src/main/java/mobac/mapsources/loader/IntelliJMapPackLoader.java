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
package mobac.mapsources.loader;

import mobac.mapsources.MapSourcesManager;
import mobac.program.interfaces.MapSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * For map sources debugging inside IntelliJ. Allows to load the map sources
 * directly from program class path instead of the map packs.
 */
public class IntelliJMapPackLoader {

	private final Logger log = LoggerFactory.getLogger(IntelliJMapPackLoader.class);

	private final MapSourcesManager mapSourcesManager;

	public IntelliJMapPackLoader(MapSourcesManager mapSourcesManager) throws IOException {
		this.mapSourcesManager = mapSourcesManager;
	}

	public boolean loadMapPacks() throws IOException {
		int mapSourceCounter = 0;
		Iterator<MapSource> it = ServiceLoader.load(MapSource.class).iterator();
		while (it.hasNext()) {
			try {
				mapSourcesManager.addMapSource(it.next());
				mapSourceCounter++;
			} catch (Exception e) {
				log.error("Failed to load map source", e);
			}
		}
		boolean result = mapSourceCounter > 0;
		if (result) {
			log.info("Loaded {} map sources", mapSourceCounter);
		}
		return result;
	}

}
