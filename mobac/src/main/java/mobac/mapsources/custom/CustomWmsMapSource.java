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
package mobac.mapsources.custom;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlRootElement;
import mobac.exceptions.MapSourceInitializationException;
import mobac.mapsources.MapSourceTools;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Custom tile store provider for wms map sources, configurable via xml file
 *
 * @author oruxman
 */
@XmlRootElement
public class CustomWmsMapSource extends CustomMapSource {

	private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS_ENGLISH = DecimalFormatSymbols
			.getInstance(Locale.ENGLISH);
	/**
	 * tested with 1.1.1 and 1.3.0, but should work with other versions
	 */
	@XmlElement(required = true, name = "version")
	private String version = "1.1.1";
	/**
	 * no spaces allowed, must be replaced with %20 in the url
	 */
	@XmlElement(required = true, name = "layers")
	private String layers = "";
	/**
	 * the coordinate system epsg:4326 - epsg:4171 - epsg:3857(WGS84) are fully
	 * tested
	 */
	@XmlElement(required = true, name = "coordinatesystem", defaultValue = "EPSG:4326")
	private String coordinateSystem = "EPSG:4326";

	/**
	 * coordinateunit is:
	 * <ul>
	 * <li><b>degree</b> for EPSG:4326, EPSG:4171</li>
	 * <li><b>meter</b> for EPSG:3857, EPSG:900913, EPSG:3785</li>
	 * </ul>
	 */
	@XmlElement(required = false, name = "coordinateunit", defaultValue = "degree")
	private CoordinateUnit coordinateUnit = CoordinateUnit.DEGREE;
	/**
	 * some wms needs more parameters: &amp;EXCEPTIONS=BLANK&amp;Styles= .....
	 */
	@XmlElement(required = false, name = "aditionalparameters")
	private String additionalParameters = "";

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		if (!"1.3.0".equals(version) && !"1.1.1".equals(version)) {
			log.warn("Unsupported WMS version found in map \"{}\": {}. MOBAC has only been tested "
					+ "with WMS version 1.1.1 and 1.3.0", getName(), version);
		}
	}

	private static double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	private static double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

	private static double lon2mercator(double l) {
		return (l * 20037508.34d / 180);
	}

	private static double lat2mercator(double l) {
		double r = Math.toRadians(l);
		double lat = Math.log((1 + Math.sin(r)) / (1 - Math.sin(r)));
		return (lat * 20037508.34d / 2 / Math.PI);
	}

	/**
	 * "WGS 84 / Pseudo-Mercator" (EPSG:3857) - "GOOGLE" (EPSG:900913) - "Popular
	 * Visualization CRS / Mercator" (EPSG:3785)
	 */
	private static String mercatorTileEdges(int x, int y, int zoom) {
		return d2s(lon2mercator(tile2lon(x, zoom)), zoom) + "," + // west (m)
				d2s(lat2mercator(tile2lat(y + 1, zoom)), zoom) + "," + // south (m)
				d2s(lon2mercator(tile2lon(x + 1, zoom)), zoom) + "," + // east (m)
				d2s(lat2mercator(tile2lat(y, zoom)), zoom); // north (m)
	}

	/**
	 * Double to String - prevents scientific notation
	 */
	private static String d2s(double value, int zoom) {
		DecimalFormat df = new DecimalFormat("#", DECIMAL_FORMAT_SYMBOLS_ENGLISH);
		int digits = 4;
		if (zoom > 10) {
			digits = 8;
		}
		df.setMaximumFractionDigits(digits);
		return df.format(value);
	}

	public String getVersion() {
		return version;
	}

	public String getLayers() {
		return layers;
	}

	@Override
	public String getTileUrl(int zoom, int tilex, int tiley) {
		boolean version130 = "1.3.0".equals(version);
		if (coordinateUnit == CoordinateUnit.METER) {
			String coordinateSystemParameter;
			if (version130) {
				// version 1.3.0 expected
				coordinateSystemParameter = "&CRS=" + coordinateSystem;
			} else {
				coordinateSystemParameter = "&SRS=" + coordinateSystem;
			}
			String url = this.url + "REQUEST=GetMap" + "&LAYERS=" + layers + coordinateSystemParameter + "&VERSION="
					+ version + "&FORMAT=image/" + tileType.getMimeType() + "&BBOX="
					+ mercatorTileEdges(tilex, tiley, zoom) + "&WIDTH=256&HEIGHT=256" + additionalParameters;
			return url;
		}
		double[] coords = MapSourceTools.calculateLatLon(this, zoom, tilex, tiley);
		String lonMin = d2s(coords[0], zoom);
		String latMin = d2s(coords[1], zoom);
		String lonMax = d2s(coords[2], zoom);
		String latMax = d2s(coords[3], zoom);
		String url = this.url + "REQUEST=GetMap" + "&LAYERS=" + layers + "&VERSION=" + version + "&FORMAT=image/"
				+ tileType.getMimeType();
		if (version130) {
			// version 1.3.0 expected
			url += "&CRS=" + coordinateSystem + "&BBOX=" + latMin + "," + lonMin + "," + latMax + "," + lonMax;
		} else {
			url += "&SRS=" + coordinateSystem + "&BBOX=" + lonMin + "," + latMin + "," + lonMax + "," + latMax;
		}
		url += "&WIDTH=256&HEIGHT=256" + additionalParameters;
		return url;
	}

	public String getCoordinateSystem() {
		return coordinateSystem;
	}

	public void applyChangesFrom(CustomWmsMapSource reloadedMapSource) throws MapSourceInitializationException {
		super.applyChangesFrom(reloadedMapSource);
		version = reloadedMapSource.version;
		layers = reloadedMapSource.layers;
		coordinateSystem = reloadedMapSource.coordinateSystem;
		additionalParameters = reloadedMapSource.additionalParameters;
	}

	@XmlEnum
	public enum CoordinateUnit {
		DEGREE, METER
	}
}
