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
package mobac.mapsources.mappacks.openstreetmap;

import java.net.HttpURLConnection;

public class Osm4uMaps extends AbstractOsmMapSource {

	public static final String NAME = "4uMaps";
	private static final String SERVER = "https://tileserver.4umaps.com";

	public Osm4uMaps() {
		super(NAME);
		minZoom = 2;
		maxZoom = 15;
		tileUpdate = TileUpdate.IfNoneMatch;
	}

	@Override
	public String getTileUrl(int zoom, int tilex, int tiley) {
		return SERVER + super.getTileUrl(zoom, tilex, tiley);
	}

	@Override
	public String toString() {
		return "OpenStreetMap 4UMaps.eu";
	}

	@Override
	protected void prepareTileUrlConnection(HttpURLConnection conn) {
		super.prepareTileUrlConnection(conn);
		conn.setInstanceFollowRedirects(true);
	}

}
