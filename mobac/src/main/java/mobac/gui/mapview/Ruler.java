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
package mobac.gui.mapview;

import mobac.program.interfaces.MapSpace;
import mobac.program.model.Settings;
import mobac.program.model.UnitSystem;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

// License: GPL. Copyright 2019 by Pierre Reinert
// Measurement tool

public class Ruler {
	private static final Font font = new Font("SansSerif", Font.BOLD, 12);
	private final PreviewMap map;
	public ArrayList<Point2D.Double> segments = new ArrayList<>();

	public Ruler(PreviewMap map) {
		super();
		this.map = map;
	}

	public Point2D.Double first() {
		return segments.get(0);
	}

	public Point2D.Double last() {
		return segments.get(segments.size() - 1);
	}

	public void remove() {
		if (!segments.isEmpty()) {
			segments.remove(segments.size() - 1);
		}
	}

	public void insert(Point pix) {
		int zoom = map.getZoom();
		double lon = map.getMapSource().getMapSpace().cXToLon(pix.x, zoom);
		double lat = map.getMapSource().getMapSpace().cYToLat(pix.y, zoom);
		segments.add(new Point2D.Double(lon, lat));
		if (segments.size() == 1) {
			insert(pix); // initial 0m segment
		}
	}

	public void paint(JComponent c, Graphics2D g, Point tlc, int zoom) {
		double dist = 0d;
		MapSpace mapspace = map.getMapSource().getMapSpace();
		g.setStroke(new BasicStroke(3.0f));
		g.setColor(Color.RED);
		if (segments.size() <= 1) {
			drawDistance(g, 0d);
			return;
		}

		Point2D.Double last = null;
		for (Point2D.Double p : segments) {
			if (last != null) {
				g.drawLine(mapspace.cLonToX(last.x, zoom) - tlc.x, mapspace.cLatToY(last.y, zoom) - tlc.y,
						mapspace.cLonToX(p.x, zoom) - tlc.x, mapspace.cLatToY(p.y, zoom) - tlc.y);
				dist += mapspace.distance(last.y, last.x, p.y, p.x);
			}
			last = p;
		}

		drawDistance(g, dist);
	}

	public void drawDistance(Graphics2D g, double dist) {
		UnitSystem unitSystem = Settings.getInstance().unitSystem;
		String unit = unitSystem.unitLarge;
		String value = String.format("%.2f %s", dist, unit);

		g.setBackground(Color.WHITE);
		g.setColor(Color.BLUE);
		g.clearRect(87, 80, 85, 30);
		g.drawRect(87, 80, 85, 30);
		g.setFont(font);
		g.drawString(value, 100, 100);
	}
}
