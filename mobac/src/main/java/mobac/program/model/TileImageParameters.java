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

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.awt.Dimension;

@XmlRootElement
public final class TileImageParameters implements Cloneable {

	@XmlAnyAttribute
	protected AnyAttributeMap attr = new AnyAttributeMap();

	/**
	 * Default constructor as required by JAXB
	 */
	protected TileImageParameters() {
		super();
	}

	private TileImageParameters(AnyAttributeMap attrMap) {
		attr.putAll(attrMap);
	}

	public TileImageParameters(int width, int height, TileImageFormat format) {
		super();
		attr.setAttr("format", format.name());
		attr.setInt("height", height);
		attr.setInt("width", width);
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		// read all values once for detecting problems
		attr.getInt(Name.height.name());
		attr.getInt(Name.width.name());
		TileImageFormat.valueOf(attr.getAttr("format"));
	}

	public int getWidth() {
		return attr.getInt("width");
	}

	public int getHeight() {
		return attr.getInt("height");
	}

	public Dimension getDimension() {
		return new Dimension(getWidth(), getHeight());
	}

	public TileImageFormat getFormat() {
		return TileImageFormat.valueOf(attr.getAttr("format"));
	}

	@Override
	public String toString() {
		return "Tile size: (" + getWidth() + "/" + getHeight() + ") " + getFormat().toString() + ")";
	}

	@Override
	public Object clone() {
		return new TileImageParameters(attr);
	}

	public enum Name {
		width, height, format, format_png, format_jpg
	}

}
