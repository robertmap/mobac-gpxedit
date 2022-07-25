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
 * Alpine Quest Map : https://alpinequest.net/ Developer :
 * ph-t@users.sourceforge.net
 */
public class MetaDataLevel {
	public final long metaDataByteIndex;
	private final List<MetaDataTile> tileList;
	public long byteIndex;

	MetaDataLevel(long metaDataByteIndex) {
		this.metaDataByteIndex = metaDataByteIndex;
		this.byteIndex = -1;
		this.tileList = new ArrayList<>();
	}

	public List<MetaDataTile> getTileList() {
		return tileList;
	}

	public void addTile(MetaDataTile tile) {
		tileList.add(tile);
	}
}
