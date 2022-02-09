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
package mobac.program.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.awt.Point;
import java.awt.Polygon;
import java.util.Vector;

/**
 * Required {@link XmlAdapter} implementation for serializing a {@link Polygon}
 */
public class PolygonAdapter extends XmlAdapter<PolygonType, Polygon> {

    @Override
    public PolygonType marshal(Polygon polygon) throws Exception {
        Vector<Point> points = new Vector<>(polygon.npoints);
        for (int i = 0; i < polygon.npoints; i++) {
            Point p = new Point(polygon.xpoints[i], polygon.ypoints[i]);
            points.add(p);
        }
        return new PolygonType(points);
    }

    @Override
    public Polygon unmarshal(PolygonType value) throws Exception {
        int nPoints = value.points.size();
        int[] xPoints = new int[nPoints];
        int[] yPoints = new int[nPoints];
        for (int i = 0; i < nPoints; i++) {
            Point p = value.points.get(i);
            xPoints[i] = p.x;
            yPoints[i] = p.y;
        }

        return new Polygon(xPoints, yPoints, nPoints);
    }

}
