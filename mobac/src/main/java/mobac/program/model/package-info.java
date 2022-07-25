/**
 * Copyright (c) MOBAC developers
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * <p>
 * Package level definition of adapters for JAXB
 * <p>
 * Package level definition of adapters for JAXB
 */
/**
 * Package level definition of adapters for JAXB
 */
@XmlJavaTypeAdapters({@XmlJavaTypeAdapter(value = PointAdapter.class, type = java.awt.Point.class),
		@XmlJavaTypeAdapter(value = DimensionAdapter.class, type = java.awt.Dimension.class),
		@XmlJavaTypeAdapter(value = PolygonAdapter.class, type = java.awt.Polygon.class)})
package mobac.program.model;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import mobac.program.jaxb.DimensionAdapter;
import mobac.program.jaxb.PointAdapter;
import mobac.program.jaxb.PolygonAdapter;
