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

package mobac.mapsources.custom.aqm;

import java.util.ArrayList;
import java.util.List;

/**
 * Alpine Quest Map : http://alpinequest.net/ Developer :
 * ph-t@users.sourceforge.net
 */
public class AqmLevel {
	public final List<AqmTile> tiles;
	public final int z; // zoom

	public final int id;
	public final String name;
	public final String scale;
	public final String datasource;
	public final String copyright;
	public final String projection;
	public final String geoid;
	public final int xtsize;
	public final int ytsize;
	public final double xtratio;
	public final double ytratio;
	public final double xtoffset;
	public final double ytoffset;
	public final int xtmin;
	public final int xtmax;
	public final int ytmin;
	public final int ytmax;
	public final String background;
	public final String imgformat;

	AqmLevel(AqmPropertyParser properties) {
		this.tiles = new ArrayList<>();

		this.id = properties.getIntProperty("id");
		this.z = id;
		this.name = properties.getStringProperty("name");
		this.scale = properties.getStringProperty("scale");
		this.datasource = properties.getStringProperty("datasource");
		this.copyright = properties.getStringProperty("copyright");
		this.projection = properties.getStringProperty("projection");
		this.geoid = properties.getStringProperty("geoid");
		this.xtsize = properties.getIntProperty("xtsize");
		this.ytsize = properties.getIntProperty("ytsize");
		this.xtratio = properties.getDoubleProperty("xtratio");
		this.ytratio = properties.getDoubleProperty("ytratio");
		this.xtoffset = properties.getDoubleProperty("xtoffset");
		this.ytoffset = properties.getDoubleProperty("ytoffset");
		this.xtmin = properties.getIntProperty("xtmin");
		this.xtmax = properties.getIntProperty("xtmax");
		this.ytmin = properties.getIntProperty("ytmin");
		this.ytmax = properties.getIntProperty("ytmax");
		this.background = properties.getStringProperty("background");
		this.imgformat = properties.getStringProperty("imgformat");
	}

	int getXtCenter() {
		return ((xtmin + xtmax) / 2) - 1;
	}

	int getYtCenter() {
		return ((ytmin + ytmax) / 2) + 1;
	}

}
