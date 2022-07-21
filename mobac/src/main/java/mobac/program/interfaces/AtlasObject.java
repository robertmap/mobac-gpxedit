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
package mobac.program.interfaces;

import mobac.exceptions.InvalidNameException;

/**
 * Marker interface that indicates that the implementing class/instance is an atlas or is part of an atlas (layer or
 * map)
 */
public interface AtlasObject {

    String getName();

    void setName(String newName) throws InvalidNameException;

    /**
     * Called after loading the complete atlas from a profile.
     *
     * @return any problems found? <code>true</code>=yes
     */
    boolean checkData();

    /**
     * minimum latitude (corresponds to row/y value)
     *
     * @return
     */
    double getMinLat();

    /**
     * maximum latitude (corresponds to row/y value)
     *
     * @return
     */
    double getMaxLat();

    /**
     * minimum longitude (corresponds to column/x value)
     *
     * @return
     */
    double getMinLon();

    /**
     * maximum longitude (corresponds to column/x value)
     *
     * @return
     */
    double getMaxLon();
}
