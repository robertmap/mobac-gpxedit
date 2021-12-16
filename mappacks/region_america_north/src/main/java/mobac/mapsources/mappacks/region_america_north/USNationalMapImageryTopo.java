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
package mobac.mapsources.mappacks.region_america_north;

import mobac.mapsources.AbstractHttpMapSource;
import mobac.program.model.TileImageType;

/**
 * USGSImageryTopo (MapServer)
 * https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryTopo/MapServer
 */
public class USNationalMapImageryTopo extends AbstractHttpMapSource {

	public USNationalMapImageryTopo() {
		super("USGS National Map Satellite & Topo", 0, 15, TileImageType.JPG, TileUpdate.IfNoneMatch);
	}

	private static final String BASE_URL = "https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryTopo/MapServer/tile/";

	public String getTileUrl(int zoom, int x, int y) {
		return BASE_URL + zoom + "/" + y + "/" + x;
	}
}
