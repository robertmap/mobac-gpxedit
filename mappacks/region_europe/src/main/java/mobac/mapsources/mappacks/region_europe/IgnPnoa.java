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
package mobac.mapsources.mappacks.region_europe;

import mobac.mapsources.AbstractHttpMapSource;
import mobac.program.interfaces.MapSourceTextAttribution;
import mobac.program.model.TileImageType;

/**
 *
 * https://sourceforge.net/p/mobac/feature-requests/290/
 *
 */
public class IgnPnoa extends AbstractHttpMapSource implements MapSourceTextAttribution {

	private static final String BASE_URL = "http://www.ign.es/wmts/pnoa-ma?"
			+ "layer=OI.OrthoimageCoverage&style=default&tilematrixset=GoogleMapsCompatible&"
			+ "Service=WMTS&Request=GetTile&Version=1.0.0&Format=image%2Fjpeg&";

	public IgnPnoa() {
		super("IgnPnoa", 1, 18, TileImageType.JPG, TileUpdate.LastModified);
	}

	public String getTileUrl(int zoom, int tilex, int tiley) {
		return BASE_URL + "TileMatrix=" + zoom + "&TileCol=" + tilex + "&TileRow=" + tiley;
	}

	@Override
	public String toString() {
		return "IGN PNOA WMTS (Spain)";
	}

	public String getAttributionText() {
		return "CC-BY 4.0 scne.es 2010";
	}

	public String getAttributionLinkURL() {
		return "http://www.scne.es";
	}
}
