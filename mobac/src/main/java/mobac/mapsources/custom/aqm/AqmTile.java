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

/**
 * Alpine Quest Map : http://alpinequest.net/ Developer : ph-t@users.sourceforge.net
 */

public class AqmTile {
    public String name;
    public byte[] bTile;
    public long tileByteIndex;

    public int x;
    public int y;

    AqmTile(String name, long tileByteIndex) {
        this.name = name;
        this.tileByteIndex = tileByteIndex;

        String sx = name.replaceFirst("([0-9]+)_([0-9]+)", "$1");
        this.x = Integer.parseInt(sx);

        String sy = name.replaceFirst("([0-9]+)_([0-9]+)", "$2");
        this.y = Integer.parseInt(sy);
    }

    AqmTile(String name, byte[] bTile, long tileByteIndex) {
        this(name, tileByteIndex);
        this.bTile = bTile;
    }

};