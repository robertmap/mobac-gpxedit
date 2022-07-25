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
package mobac.mapsources.mappacks.region_europe_east;

import mobac.mapsources.AbstractHttpMapSource;
import mobac.program.interfaces.MapSourceTextAttribution;
import mobac.program.model.TileImageType;

/**
 * http://www.freemap.sk
 */
public class FreemapSlovakiaCycling extends AbstractHttpMapSource implements MapSourceTextAttribution {

	private static final String[] SERVERS = {"a", "b", "c", "d"};
	private static int SERVER_NUM = 0;

	public FreemapSlovakiaCycling() {
		super("FreemapSlovakiaCyclo", 6, 16, TileImageType.PNG, TileUpdate.IfModifiedSince);
	}

	public String getTileUrl(int zoom, int tilex, int tiley) {
		String server = SERVERS[SERVER_NUM];
		SERVER_NUM = (SERVER_NUM + 1) % SERVERS.length;
		return String.format("https://%s.freemap.sk/C/%d/%d/%d.png", server, zoom, tilex, tiley);
	}

	@Override
	public String toString() {
		return "Freemap Slovakia Cycle Map";
	}

	public String getAttributionText() {
		return "© OpenStreetMap contributors, CC-BY-SA";
	}

	public String getAttributionLinkURL() {
		return "http://openstreetmap.org";
	}
}
