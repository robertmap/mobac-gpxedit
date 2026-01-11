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
package mobac.gui.panels;

import mobac.data.gpx.gpx11.RteType;
import mobac.data.gpx.gpx11.TrkType;
import mobac.data.gpx.gpx11.TrksegType;
import mobac.data.gpx.gpx11.WptType;
import mobac.gui.actions.GpxAddPoint;
import mobac.gui.actions.GpxClear;
import mobac.gui.actions.GpxLoad;
import mobac.gui.actions.GpxNew;
import mobac.gui.actions.GpxSave;
import mobac.gui.components.JCollapsiblePanel;
import mobac.gui.gpxtree.GpxEntry;
import mobac.gui.gpxtree.GpxRootEntry;
import mobac.gui.gpxtree.GpxTreeListener;
import mobac.gui.gpxtree.RteEntry;
import mobac.gui.gpxtree.TrkEntry;
import mobac.gui.gpxtree.TrksegEntry;
import mobac.gui.gpxtree.WptEntry;
import mobac.gui.mapview.PreviewMap;
import mobac.gui.mapview.layer.GpxLayer;
import mobac.utilities.GBC;
import mobac.utilities.I18nUtils;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows to load, display, edit and save gpx files using a tree view. TODO warn
 * unsaved changes on exit
 */
public class JGpxPanel extends JCollapsiblePanel {

	/**
	 * Returns the root node of the GPX tree.
	 */
	public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}

	/**
	 * Saves the current GPX session (file paths and expanded/collapsed state) to
	 * Settings.
	 */
	public void saveGpxSession() {
		mobac.program.model.Settings settings = mobac.program.model.Settings.getInstance();
		// Save file paths in order
		settings.gpxSessionFiles.clear();
		settings.gpxSessionVisible.clear();
		for (int i = 0; i < rootNode.getChildCount(); i++) {
			Object userObj = ((DefaultMutableTreeNode) rootNode.getChildAt(i)).getUserObject();
			if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
				mobac.gui.gpxtree.GpxRootEntry entry = (mobac.gui.gpxtree.GpxRootEntry) userObj;
				if (entry.getLayer() != null && entry.getLayer().getFile() != null) {
					settings.gpxSessionFiles.add(entry.getLayer().getFile().getAbsolutePath());
					settings.gpxSessionVisible.add(entry.getLayer().isVisible());
				}
			}
		}
		// Save expanded paths as strings
		settings.gpxSessionExpandedPaths.clear();
		java.util.Enumeration<TreePath> expanded = tree.getExpandedDescendants(new TreePath(rootNode));
		if (expanded != null) {
			while (expanded.hasMoreElements()) {
				TreePath path = expanded.nextElement();
				settings.gpxSessionExpandedPaths.add(treePathToString(path));
			}
		}
	}

	/**
	 * Restores the GPX session (file paths and expanded/collapsed state) from
	 * Settings.
	 */
	public void restoreGpxSession() {
		mobac.program.model.Settings settings = mobac.program.model.Settings.getInstance();
		if (settings.gpxSessionFiles == null || settings.gpxSessionFiles.isEmpty())
			return;
		// Clear current model
		resetModel();
		// Load files in order, restoring visibility if available
		List<Boolean> visibleList = settings.gpxSessionVisible;
		java.util.List<String> missingFiles = new java.util.ArrayList<>();
		for (int i = 0; i < settings.gpxSessionFiles.size(); i++) {
			String filePath = settings.gpxSessionFiles.get(i);
			java.io.File file = new java.io.File(filePath);
			if (file.exists()) {
				try {
					mobac.data.gpx.gpx11.Gpx gpx = mobac.data.gpx.GPXUtils.loadGpxFile(file);
					mobac.gui.mapview.layer.GpxLayer gpxLayer = new mobac.gui.mapview.layer.GpxLayer(gpx);
					gpxLayer.setFile(file);
					// Restore visibility if available
					if (visibleList != null && i < visibleList.size()) {
						gpxLayer.setVisible(visibleList.get(i));
					}
					addGpxLayer(gpxLayer);
				} catch (Exception ex) {
					// Ignore files that fail to load
				}
			} else {
				missingFiles.add(filePath);
			}
		}
		if (!missingFiles.isEmpty()) {
			javax.swing.SwingUtilities.invokeLater(() -> {
				javax.swing.JOptionPane.showMessageDialog(this,
						"The following GPX files could not be found and were not restored:\n"
								+ String.join("\n", missingFiles),
						"Missing GPX Files", javax.swing.JOptionPane.WARNING_MESSAGE);
			});
		}
		// Restore expanded paths
		if (settings.gpxSessionExpandedPaths != null) {
			for (String pathStr : settings.gpxSessionExpandedPaths) {
				TreePath path = stringToTreePath(pathStr);
				if (path != null)
					tree.expandPath(path);
			}
		}
	}

	// Helper: Convert TreePath to a string (by node user object toString)
	private String treePathToString(TreePath path) {
		Object[] objs = path.getPath();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < objs.length; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) objs[i];
			Object userObj = node.getUserObject();
			sb.append(userObj == null ? "null" : userObj.toString());
			if (i < objs.length - 1)
				sb.append("/");
		}
		return sb.toString();
	}

	// Helper: Convert string back to TreePath (by matching user object toString)
	private TreePath stringToTreePath(String str) {
		String[] parts = str.split("/");
		DefaultMutableTreeNode node = rootNode;
		java.util.List<DefaultMutableTreeNode> pathNodes = new java.util.ArrayList<>();
		pathNodes.add(node);
		for (int i = 1; i < parts.length; i++) {
			boolean found = false;
			for (int j = 0; j < node.getChildCount(); j++) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(j);
				Object userObj = child.getUserObject();
				if (userObj != null && userObj.toString().equals(parts[i])) {
					node = child;
					pathNodes.add(node);
					found = true;
					break;
				}
			}
			if (!found)
				return null;
		}
		return new TreePath(pathNodes.toArray());
	}
	public PreviewMap getPreviewMap() {
		return previewMap;
	}

	private static final long serialVersionUID = 1L;

	private final JTree tree;
	private final DefaultTreeModel model;
	private final PreviewMap previewMap;
	private DefaultMutableTreeNode rootNode;
	private ArrayList<String> openedFiles;

	/**
	 * Removes a file from the openedFiles list.
	 *
	 * @param path
	 *            Absolute path of the file to remove
	 */
	public void removeOpenedFile(String path) {
		openedFiles.remove(path);
	}

	/**
	 * Removes multiple nodes (waypoints, tracks, segments, routes) from the tree.
	 * Only non-root nodes are allowed.
	 */
	public static void removeMultipleNodes(javax.swing.JTree tree, java.util.List<DefaultMutableTreeNode> nodes) {
		// Defensive: sort by depth, deepest first
		nodes.sort((a, b) -> b.getLevel() - a.getLevel());
		javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) tree.getModel();
		for (DefaultMutableTreeNode node : nodes) {
			Object userObj = node.getUserObject();
			if (userObj instanceof mobac.gui.gpxtree.WptEntry) {
				mobac.gui.gpxtree.WptEntry wptEntry = (mobac.gui.gpxtree.WptEntry) userObj;
				// Ensure the node is set for correct removal
				wptEntry.setNode(node);
				if (wptEntry.getNode() != null && wptEntry.getNode().getParent() != null) {
					wptEntry.getLayer().getPanel().removeWpt(wptEntry);
				}
			} else if (userObj instanceof mobac.gui.gpxtree.TrkEntry || userObj instanceof mobac.gui.gpxtree.TrksegEntry
					|| userObj instanceof mobac.gui.gpxtree.RteEntry) {
				if (node.getParent() != null) {
					model.removeNodeFromParent(node);
				}
			}
		}
	}

	public JGpxPanel(PreviewMap previewMap) {
		super("Gpx", new GridBagLayout());

		this.previewMap = previewMap;

		JButton newGpx = new JButton(I18nUtils.localizedStringForKey("rp_gpx_new_gpx"));
		newGpx.addActionListener(new GpxNew(this));

		JButton loadGpx = new JButton(I18nUtils.localizedStringForKey("rp_gpx_load_gpx"));
		loadGpx.addActionListener(new GpxLoad(this));

		JButton saveGpx = new JButton(I18nUtils.localizedStringForKey("rp_gpx_save_gpx"));
		saveGpx.addActionListener(e -> {
			int answer = javax.swing.JOptionPane.showConfirmDialog(null,
					"Saving will overwrite the file and deleted data will be lost.\nContinue?", "Confirm Save",
					javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
			if (answer == javax.swing.JOptionPane.YES_OPTION) {
				new GpxSave(this).actionPerformed(null);
			}
		});

		JButton clearGpx = new JButton(I18nUtils.localizedStringForKey("rp_gpx_clear_gpx"));
		clearGpx.addActionListener(new GpxClear(this));

		JButton addPointGpx = new JButton(I18nUtils.localizedStringForKey("rp_gpx_add_wpt"));
		addPointGpx.addActionListener(new GpxAddPoint(this));

		// Root node is a dummy node, not a GpxRootEntry or String representing a file
		rootNode = new DefaultMutableTreeNode("GPX Files");
		tree = new JTree(rootNode);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		// Prevent selecting GPX file nodes with other nodes
		tree.addTreeSelectionListener(e -> {
			javax.swing.tree.TreePath[] paths = tree.getSelectionPaths();
			if (paths == null || paths.length <= 1)
				return;
			boolean hasRoot = false;
			int rootIdx = -1;
			for (int i = 0; i < paths.length; i++) {
				javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) paths[i]
						.getLastPathComponent();
				Object userObj = node.getUserObject();
				if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
					hasRoot = true;
					rootIdx = i;
					break;
				}
			}
			if (hasRoot) {
				// Only allow the root node to remain selected
				tree.setSelectionPath(paths[rootIdx]);
			}
		});
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		JScrollPane treeView = new JScrollPane(tree);
		// treeView.setPreferredSize(new Dimension(100, 300));
		model = (DefaultTreeModel) tree.getModel();

		tree.addMouseListener(new GpxTreeListener());

		// --- Custom renderer for GPX file nodes (JLabel only) ---
		// Load icons once
		final javax.swing.ImageIcon iconFolderClosed = new javax.swing.ImageIcon(
				getClass().getResource("/mobac/resources/images/icon_folder_closed.png"));
		final javax.swing.ImageIcon iconFolderClosedInvisible = new javax.swing.ImageIcon(
				getClass().getResource("/mobac/resources/images/icon_folder_closed_invisible.png"));
		final javax.swing.ImageIcon iconFolderClosedDirty = new javax.swing.ImageIcon(
				getClass().getResource("/mobac/resources/images/icon_folder_closed_dirty.png"));
		final javax.swing.ImageIcon iconFolderClosedInvisibleDirty = new javax.swing.ImageIcon(
				getClass().getResource("/mobac/resources/images/icon_folder_closed_invisible_dirty.png"));

		tree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
			@Override
			public java.awt.Component getTreeCellRendererComponent(javax.swing.JTree tree, Object value,
					boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				javax.swing.JLabel label = (javax.swing.JLabel) super.getTreeCellRendererComponent(tree, value,
						selected, expanded, leaf, row, hasFocus);
				if (value instanceof javax.swing.tree.DefaultMutableTreeNode) {
					Object userObj = ((javax.swing.tree.DefaultMutableTreeNode) value).getUserObject();
					if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
						mobac.gui.gpxtree.GpxRootEntry entry = (mobac.gui.gpxtree.GpxRootEntry) userObj;
						// Choose icon based on visible/dirty state
						if (entry.getLayer().isVisible()) {
							if (entry.isDirty()) {
								label.setIcon(iconFolderClosedDirty);
							} else {
								label.setIcon(iconFolderClosed);
							}
						} else {
							if (entry.isDirty()) {
								label.setIcon(iconFolderClosedInvisibleDirty);
							} else {
								label.setIcon(iconFolderClosedInvisible);
							}
						}
						label.setText(entry.toString());
					}
				}
				return label;
			}
		});

		openedFiles = new ArrayList<>();

		GBC eol = GBC.eol().fill(GBC.HORIZONTAL);
		GBC std = GBC.std().fill(GBC.HORIZONTAL);
		addContent(treeView, GBC.eol().fill());
		addContent(clearGpx, std);
		addContent(addPointGpx, eol);
		addContent(newGpx, std);
		addContent(loadGpx, std);
		addContent(saveGpx, eol);
	}

	/**
	 * adds a layer for a new gpx file on the map and adds its structure to the
	 * treeview
	 */
	public GpxRootEntry addGpxLayer(GpxLayer layer) {
		GpxRootEntry gpxEntry = new GpxRootEntry(layer);
		// Mark as dirty if this is a new file (no file assigned yet)
		if (layer.getFile() == null) {
			gpxEntry.setDirty(true);
		}
		layer.setPanel(this);
		DefaultMutableTreeNode gpxNode = new DefaultMutableTreeNode(gpxEntry);
		gpxEntry.setNode(gpxNode); // Ensure node is set for GpxRootEntry
		model.insertNodeInto(gpxNode, rootNode, rootNode.getChildCount());
		TreePath path = new TreePath(gpxNode.getPath());
		tree.scrollPathToVisible(new TreePath(path));
		tree.setSelectionPath(path);

		addRtes(layer, gpxNode);
		addTrks(layer, gpxNode);
		addWpts(layer, gpxNode);

		if (layer.getFile() != null) {
			openedFiles.add(layer.getFile().getAbsolutePath());
		}

		previewMap.mapLayers.add(layer);
		return gpxEntry;
	}

	/**
	 * @param layer
	 * @param gpxNode
	 */
	public void addWpts(GpxLayer layer, DefaultMutableTreeNode gpxNode) {
		List<WptType> wpts = layer.getGpx().getWpt();
		for (WptType wpt : wpts) {
			WptEntry wptEntry = new WptEntry(wpt, layer);
			DefaultMutableTreeNode wptNode = new DefaultMutableTreeNode(wptEntry);
			wptEntry.setNode(wptNode);
			model.insertNodeInto(wptNode, gpxNode, gpxNode.getChildCount());
		}
	}

	/**
	 * @param layer
	 * @param gpxNode
	 */
	private void addTrks(GpxLayer layer, DefaultMutableTreeNode gpxNode) {
		// tracks
		List<TrkType> trks = layer.getGpx().getTrk();
		for (TrkType trk : trks) {
			TrkEntry trkEntry = new TrkEntry(trk, layer);
			DefaultMutableTreeNode trkNode = new DefaultMutableTreeNode(trkEntry);
			trkEntry.setNode(trkNode);
			model.insertNodeInto(trkNode, gpxNode, gpxNode.getChildCount());
			// trkseg
			List<TrksegType> trksegs = trk.getTrkseg();
			int counter = 1;
			for (TrksegType trkseg : trksegs) {
				TrksegEntry trksegEntry = new TrksegEntry(trkseg, counter, layer);
				DefaultMutableTreeNode trksegNode = new DefaultMutableTreeNode(trksegEntry);
				trksegEntry.setNode(trksegNode);
				model.insertNodeInto(trksegNode, trkNode, trkNode.getChildCount());
				counter++;

				// add trkpts
				List<WptType> trkpts = trkseg.getTrkpt();
				for (WptType trkpt : trkpts) {
					WptEntry trkptEntry = new WptEntry(trkpt, layer);
					DefaultMutableTreeNode trkptNode = new DefaultMutableTreeNode(trkptEntry);
					trkptEntry.setNode(trkptNode);
					model.insertNodeInto(trkptNode, trksegNode, trksegNode.getChildCount());
				}
			}
		}
	}

	/**
	 * adds routes and route points to the tree view
	 *
	 * @param layer
	 * @param gpxNode
	 */
	private void addRtes(GpxLayer layer, DefaultMutableTreeNode gpxNode) {
		List<RteType> rtes = layer.getGpx().getRte();
		for (RteType rte : rtes) {
			RteEntry rteEntry = new RteEntry(rte, layer);
			DefaultMutableTreeNode rteNode = new DefaultMutableTreeNode(rteEntry);
			rteEntry.setNode(rteNode);
			model.insertNodeInto(rteNode, gpxNode, gpxNode.getChildCount());
			// add rtepts
			List<WptType> rtepts = rte.getRtept();
			for (WptType rtept : rtepts) {
				WptEntry rteptEntry = new WptEntry(rtept, layer);
				DefaultMutableTreeNode rteptNode = new DefaultMutableTreeNode(rteptEntry);
				rteptEntry.setNode(rteptNode);
				model.insertNodeInto(rteptNode, rteNode, rteNode.getChildCount());
			}
		}
	}

	/**
	 * Updates the tree view to show the newly added waypoint.
	 *
	 * @param wpt
	 *            - new waypoint
	 * @param gpxEntry
	 *            - parent entry in the tree
	 */
	public void addWpt(WptType wpt, GpxEntry gpxEntry) {
		WptEntry wptEntry = new WptEntry(wpt, gpxEntry.getLayer());
		DefaultMutableTreeNode wptNode = new DefaultMutableTreeNode(wptEntry);
		model.insertNodeInto(wptNode, gpxEntry.getNode(), gpxEntry.getNode().getChildCount());
		// Mark the nearest ancestor GPX file node as dirty and update node
		DefaultMutableTreeNode node = gpxEntry.getNode();
		DefaultMutableTreeNode gpxFileNode = node;
		while (gpxFileNode != null) {
			Object userObj = gpxFileNode.getUserObject();
			if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
				mobac.gui.gpxtree.GpxRootEntry rootEntry = (mobac.gui.gpxtree.GpxRootEntry) userObj;
				rootEntry.setDirty(true);
				model.nodeChanged(gpxFileNode);
				break;
			}
			gpxFileNode = (DefaultMutableTreeNode) gpxFileNode.getParent();
		}
	}

	/**
	 * Updates the tree view after removing a waypoint.
	 *
	 * @param wptEntry
	 *            - deleted waypoint
	 */
	public void removeWpt(WptEntry wptEntry) {
		DefaultMutableTreeNode wptNode = wptEntry.getNode();
		if (wptNode == null || wptNode.getParent() == null) {
			// Node already removed or not attached
			return;
		}
		// Remove from GPX data model
		GpxLayer layer = wptEntry.getLayer();
		if (layer != null) {
			mobac.data.gpx.gpx11.Gpx gpx = layer.getGpx();
			gpx.getWpt().remove(wptEntry.getWpt());
		}
		DefaultMutableTreeNode gpxFileNode = wptNode;
		// Find the parent GPX file node
		while (gpxFileNode != null) {
			Object userObj = gpxFileNode.getUserObject();
			if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
				mobac.gui.gpxtree.GpxRootEntry rootEntry = (mobac.gui.gpxtree.GpxRootEntry) userObj;
				rootEntry.setDirty(true);
				model.nodeChanged(gpxFileNode);
				// Remove the waypoint, then select the GPX file node
				model.removeNodeFromParent(wptNode);
				TreePath path = new TreePath(gpxFileNode.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				return;
			}
			gpxFileNode = (DefaultMutableTreeNode) gpxFileNode.getParent();
		}
		// Fallback: just remove if no GPX file node found
		if (wptNode.getParent() != null) {
			model.removeNodeFromParent(wptNode);
		}
	}

	public GpxEntry getSelectedEntry() {
		TreePath selection = tree.getSelectionPath();
		if (selection == null) {
			return null;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) selection.getLastPathComponent();
		// Walk up to the nearest GpxRootEntry
		while (node != null) {
			Object userObj = node.getUserObject();
			if (userObj instanceof GpxRootEntry) {
				GpxRootEntry rootEntry = (GpxRootEntry) userObj;
				rootEntry.setNode(node);
				return rootEntry;
			}
			node = (DefaultMutableTreeNode) node.getParent();
		}
		return null;
	}

	public boolean isFileOpen(String path) {
		return openedFiles.contains(path);
	}

	/**
	 * Resets the tree view. Used by GpxClear.
	 */
	public void resetModel() {
		rootNode = new DefaultMutableTreeNode("GPX Files");
		model.setRoot(rootNode);
		openedFiles = new ArrayList<>();
	}

	public DefaultTreeModel getTreeModel() {
		return model;
	}

}
