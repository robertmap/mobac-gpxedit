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

import mobac.program.model.TileImageParameters;

import javax.swing.tree.TreeNode;
import java.awt.Dimension;
import java.awt.Point;

public interface MapInterface extends AtlasObject, CapabilityDeletable, TreeNode {

    Point getMinTileCoordinate();

    Point getMaxTileCoordinate();

    int getZoom();

    MapSource getMapSource();

    Dimension getTileSize();

    LayerInterface getLayer();

    void setLayer(LayerInterface layer);

    TileImageParameters getParameters();

    void setParameters(TileImageParameters p);

    long calculateTilesToDownload();

    String getInfoText();

    TileFilter getTileFilter();

    MapInterface deepClone(LayerInterface newLayer);

}
