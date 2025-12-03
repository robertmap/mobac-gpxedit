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
package mobac.program.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import mobac.mapsources.MapSourcesManager;
import mobac.program.interfaces.MapSource;

@XmlAccessorType(XmlAccessType.FIELD)
public class Bookmark extends EastNorthCoordinate {

	@XmlAttribute
	protected int zoom;

	@XmlAttribute
	protected String name;

	@XmlAttribute
	protected String mapSource;

	/**
	 * Needed for JAXB
	 */
	@SuppressWarnings("unused")
	private Bookmark() {

	}

	public Bookmark(MapSource mapSource, int zoom, int pixelCoordinateX, int pixelCoordinateY) {
		this(null, mapSource, zoom, pixelCoordinateX, pixelCoordinateY);
	}

	public Bookmark(String name, MapSource mapSource, int zoom, int pixelCoordinateX, int pixelCoordinateY) {
		super(mapSource.getMapSpace(), zoom, pixelCoordinateX, pixelCoordinateY);
		this.mapSource = mapSource.getName();
		this.zoom = zoom;
		this.name = name;
	}

	public MapSource getMapSource() {
		return MapSourcesManager.getInstance().getSourceByName(mapSource);
	}

	public int getZoom() {
		return zoom;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name != null && name.trim().isEmpty()) {
			name = null;
		}
		this.name = name;
	}

	@Override
	public String toString() {
		if (name != null) {
			return name;
		}
		return String.format("%s at lat=%.3f lon=%.3f (zoom = %d)", mapSource, lat, lon, zoom);
	}

}
