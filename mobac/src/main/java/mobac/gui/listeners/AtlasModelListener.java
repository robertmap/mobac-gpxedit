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
 */
/**
 *
 */
package mobac.gui.listeners;

import mobac.gui.atlastree.JAtlasTree;
import mobac.gui.panels.JProfilesPanel;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public class AtlasModelListener implements TreeModelListener {

	final JAtlasTree atlasTree;
	final JProfilesPanel profilesPanel;

	public AtlasModelListener(JAtlasTree atlasTree, JProfilesPanel profilesPanel) {
		super();
		this.atlasTree = atlasTree;
		this.profilesPanel = profilesPanel;
	}

	protected void changed() {
		profilesPanel.getSaveAsButton().setEnabled(atlasTree.getAtlas().getLayerCount() > 0);
	}

	public void treeNodesChanged(TreeModelEvent e) {
		changed();
	}

	public void treeNodesInserted(TreeModelEvent e) {
		changed();
	}

	public void treeNodesRemoved(TreeModelEvent e) {
		changed();
	}

	public void treeStructureChanged(TreeModelEvent e) {
		changed();
	}
}
