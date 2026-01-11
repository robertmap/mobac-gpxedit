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
package mobac.gui.actions;

import mobac.gui.MainGUI;
import mobac.gui.mapview.interfaces.MapLayer;
import mobac.gui.mapview.layer.GpxLayer;
import mobac.gui.panels.JGpxPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

/**
 * Deletes all loaded {@link GpxLayer}s from the main map viewer.
 */
public class GpxClear implements ActionListener {

	private final JGpxPanel panel;

	public GpxClear(JGpxPanel panel) {
		super();
		this.panel = panel;
	}

	public void actionPerformed(ActionEvent e) {
		// Check for dirty GPX files before clearing
		boolean hasDirty = false;
		Iterator<MapLayer> mapLayersCheck = MainGUI.getMainGUI().previewMap.mapLayers.iterator();
		while (mapLayersCheck.hasNext()) {
			MapLayer layer = mapLayersCheck.next();
			if (layer instanceof GpxLayer) {
				GpxLayer gpxLayer = (GpxLayer) layer;
				if (gpxLayer.getPanel() != null && gpxLayer.getPanel().getTreeModel() != null) {
					javax.swing.tree.TreeModel model = gpxLayer.getPanel().getTreeModel();
					javax.swing.tree.TreeNode root = (javax.swing.tree.TreeNode) model.getRoot();
					for (int i = 0; i < root.getChildCount(); i++) {
						Object userObj = ((javax.swing.tree.DefaultMutableTreeNode) root.getChildAt(i)).getUserObject();
						if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
							if (((mobac.gui.gpxtree.GpxRootEntry) userObj).isDirty()) {
								hasDirty = true;
								break;
							}
						}
					}
				}
			}
			if (hasDirty) break;
		}
		if (hasDirty) {
			int result = javax.swing.JOptionPane.showConfirmDialog(null,
				"There are unsaved GPX files. Clear anyway and lose all unsaved changes?",
				"Unsaved Changes",
				javax.swing.JOptionPane.YES_NO_OPTION,
				javax.swing.JOptionPane.WARNING_MESSAGE);
			if (result != javax.swing.JOptionPane.YES_OPTION) {
				return;
			}
		}
		// Proceed to clear all GPX layers
		Iterator<MapLayer> mapLayers = MainGUI.getMainGUI().previewMap.mapLayers.iterator();
		while (mapLayers.hasNext()) {
			if (mapLayers.next() instanceof GpxLayer) {
				mapLayers.remove();
			}
		}
		panel.resetModel();
		MainGUI.getMainGUI().previewMap.repaint();
	}

}
