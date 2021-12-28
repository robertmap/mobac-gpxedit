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

import jakarta.xml.bind.annotation.XmlElement;
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

    private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS_ENGLISH = DecimalFormatSymbols.getInstance(Locale.ENGLISH);

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
     * the coordinate system epsg:4326 - epsg:4171 - epsg:3857(WGS84) are fully tested
     */
    @XmlElement(required = true, name = "coordinatesystem", defaultValue = "EPSG:4326")
    private String coordinateSystem = "EPSG:4326";

    /**
     * required=false for backward compatibility
     */
    @XmlElement(required = false, name = "wgs84", defaultValue = "false")
    private boolean wgs84 = false;

    /**
     * some wms needs more parameters: &amp;EXCEPTIONS=BLANK&amp;Styles= .....
     */
    @XmlElement(required = false, name = "aditionalparameters")
    private String additionalParameters = "";

    private static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    public String getVersion() {
        return version;
    }

    public String getLayers() {
        return layers;
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
     * "WGS 84 / Pseudo-Mercator" (EPSG:3857) - "GOOGLE" (EPSG:900913) - "Popular Visualization CRS / Mercator" (EPSG:3785)
     */
    private static String MercatorTileEdges(int x, int y, int zoom) {
        return d2s(lon2mercator(tile2lon(x, zoom))) + "," +    // west (m)
                d2s(lat2mercator(tile2lat(y + 1, zoom))) + "," +    // south (m)
                d2s(lon2mercator(tile2lon(x + 1, zoom))) + "," +    // east (m)
                d2s(lat2mercator(tile2lat(y, zoom)));        // north (m)
    }

    /**
     * Double to String - prevents scientific notation
     */
    private static String d2s(double value) {
        DecimalFormat df = new DecimalFormat("#", DECIMAL_FORMAT_SYMBOLS_ENGLISH);
        df.setMaximumFractionDigits(8);
        return df.format(value);
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        if (wgs84) {
            String coordinateSystemParameter;
            if ("1.1.1".equals(version)) {
                coordinateSystemParameter = "&SRS=" + coordinateSystem;
            } else {
                // version 1.3.0 expected
                coordinateSystemParameter = "&CRS=" + coordinateSystem;
            }
            String url = this.url + "REQUEST=GetMap" + "&LAYERS=" + layers + coordinateSystemParameter + "&VERSION="
                    + version + "&FORMAT=image/" + tileType.getMimeType() + "&BBOX="
                    + MercatorTileEdges(tilex, tiley, zoom) + "&WIDTH=256&HEIGHT=256" + additionalParameters;
            return url;
        }
        double[] coords = MapSourceTools.calculateLatLon(this, zoom, tilex, tiley);
        String lonMin = d2s(coords[0]);
        String latMin = d2s(coords[1]);
        String lonMax = d2s(coords[2]);
        String latMax = d2s(coords[3]);
        String url = this.url + "REQUEST=GetMap" + "&LAYERS=" + layers + "&VERSION="
                + version + "&FORMAT=image/" + tileType.getMimeType();
        if ("1.1.1".equals(version)) {
            url += "&SRS=" + coordinateSystem + "&BBOX=" + lonMin + "," + latMin + "," + lonMax + "," + latMax;
        } else {
            // version 1.3.0 expected
            url += "&CRS=" + coordinateSystem + "&BBOX=" + latMin + "," + lonMin + "," + latMax + "," + lonMax;
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
}
