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
package mobac.gui.gpxtree;

import mobac.gui.actions.GpxElementListener;
import mobac.utilities.I18nUtils;
import mobac.gui.mapview.layer.GpxLayer;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Listener for the gpx editor tree.
 *
 * @author lhoeppner
 */
public class GpxTreeListener implements MouseListener {

	public void actionPerformed(ActionEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getSource() instanceof JTree) {
			JTree tree = (JTree) e.getSource();
			int x = e.getX();
			int y = e.getY();
			TreePath path = tree.getPathForLocation(x, y);
			if (path == null)
				return;
			// Only set selection if neither Ctrl nor Shift is pressed
			int mods = e.getModifiersEx();
			boolean ctrl = (mods & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0;
			boolean shift = (mods & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0;
			if (!ctrl && !shift) {
				tree.setSelectionPath(path);
			}
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			Object userObj = node.getUserObject();
			if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
				// Only toggle visibility if click is within the icon bounds (leftmost 0-20px of
				// the row)
				int row = tree.getRowForPath(path);
				java.awt.Rectangle rowBounds = tree.getRowBounds(row);
				// DefaultTreeCellRenderer icon is at x=rowBounds.x, width ~16px (icon size)
				if (rowBounds != null && x - rowBounds.x >= 0 && x - rowBounds.x <= 20) {
					mobac.gui.gpxtree.GpxRootEntry entry = (mobac.gui.gpxtree.GpxRootEntry) userObj;
					entry.getLayer().setVisible(!entry.getLayer().isVisible());
					tree.repaint();
					if (entry.getLayer().getPanel() != null && entry.getLayer().getPanel().getPreviewMap() != null) {
						entry.getLayer().getPanel().getPreviewMap().repaint();
					}
				}
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger()) {
			showPopup(e);
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			showPopup(e);
		}
	}

	// --- Utility methods for bounds and centering ---
	// Center and zoom map to bounds: [minLat, maxLat, minLon, maxLon]
	private void centerMapToBounds(double[] bounds) {
		double minLat = bounds[0], maxLat = bounds[1], minLon = bounds[2], maxLon = bounds[3];
		double centerLat = (minLat + maxLat) / 2.0;
		double centerLon = (minLon + maxLon) / 2.0;
		var map = mobac.gui.MainGUI.getMainGUI().previewMap;
		var mapSource = map.getMapSource();
		var mapSpace = mapSource.getMapSpace();
		int mapWidth = map.getWidth();
		int mapHeight = map.getHeight();
		int bestZoom = mapSource.getMaxZoom();
		for (int zoom = mapSource.getMaxZoom(); zoom >= mapSource.getMinZoom(); zoom--) {
			int x1 = mapSpace.cLonToX(minLon, zoom);
			int y1 = mapSpace.cLatToY(maxLat, zoom); // Note: y is inverted
			int x2 = mapSpace.cLonToX(maxLon, zoom);
			int y2 = mapSpace.cLatToY(minLat, zoom);
			int boxWidth = Math.abs(x2 - x1);
			int boxHeight = Math.abs(y2 - y1);
			if (boxWidth <= mapWidth * 0.9 && boxHeight <= mapHeight * 0.9) {
				bestZoom = zoom;
				break;
			}
		}
		map.setDisplayPositionByLatLon(centerLat, centerLon, bestZoom);
	}

	/**
	 * Popup for all elements in the gpx tree. TODO separate for waypoints, files,
	 * tracks and routes
	 *
	 * @param e
	 */
	private void showPopup(MouseEvent e) {
		JTree tree = (JTree) e.getSource();
		TreePath[] selectedPaths = tree.getSelectionPaths();
		// If multiple nodes are selected, check if all are waypoints
		if (selectedPaths != null && selectedPaths.length > 1) {
			boolean allWaypoints = true;
			for (TreePath path : selectedPaths) {
				javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) path
						.getLastPathComponent();
				Object userObj = node.getUserObject();
				if (!(userObj instanceof mobac.gui.gpxtree.WptEntry)) {
					allWaypoints = false;
					break;
				}
			}
			JPopupMenu popup = new JPopupMenu();
			if (allWaypoints) {
				JMenuItem copyMove = new JMenuItem("Copy/Move..."); // TODO: localize
				copyMove.addActionListener(ev -> {
					// Use the same logic as the single waypoint Copy/Move, but for all selected
					javax.swing.JTree gpxTree = (javax.swing.JTree) ((javax.swing.JPopupMenu) popup).getInvoker();
					javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) gpxTree.getModel();
					javax.swing.tree.DefaultMutableTreeNode root = (javax.swing.tree.DefaultMutableTreeNode) model
							.getRoot();
					java.util.List<GpxRootEntry> gpxFiles = new java.util.ArrayList<>();
					for (int i = 0; i < root.getChildCount(); i++) {
						Object uo = ((javax.swing.tree.DefaultMutableTreeNode) root.getChildAt(i)).getUserObject();
						if (uo instanceof GpxRootEntry) {
							gpxFiles.add((GpxRootEntry) uo);
						}
					}
					String[] gpxNames = new String[gpxFiles.size() + 1];
					for (int i = 0; i < gpxFiles.size(); i++) {
						gpxNames[i] = gpxFiles.get(i).toString();
					}
					gpxNames[gpxFiles.size()] = "<New GPX file>";

					// Collect selected waypoints BEFORE any tree selection changes
					javax.swing.tree.TreePath[] selPaths = gpxTree.getSelectionPaths();
					if (selPaths == null)
						return;
					java.util.List<WptEntry> wptsToCopy = new java.util.ArrayList<>();
					java.util.List<DefaultMutableTreeNode> selectedNodes = new java.util.ArrayList<>();
					for (javax.swing.tree.TreePath path2 : selPaths) {
						Object uo = ((javax.swing.tree.DefaultMutableTreeNode) path2.getLastPathComponent())
								.getUserObject();
						if (uo instanceof WptEntry) {
							wptsToCopy.add((WptEntry) uo);
							selectedNodes.add((DefaultMutableTreeNode) path2.getLastPathComponent());
						}
					}
					if (wptsToCopy.isEmpty())
						return;

					javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(0, 1));
					javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
					javax.swing.JRadioButton copyBtn = new javax.swing.JRadioButton("Copy", true);
					javax.swing.JRadioButton moveBtn = new javax.swing.JRadioButton("Move");
					group.add(copyBtn);
					group.add(moveBtn);
					panel.add(copyBtn);
					panel.add(moveBtn);
					panel.add(new javax.swing.JLabel("Destination GPX file:"));
					javax.swing.JComboBox<String> gpxCombo = new javax.swing.JComboBox<>(gpxNames);
					panel.add(gpxCombo);

					int result = javax.swing.JOptionPane.showConfirmDialog(null, panel, "Copy/Move Waypoint(s)",
							javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
					if (result != javax.swing.JOptionPane.OK_OPTION)
						return;

					int destIdx = gpxCombo.getSelectedIndex();
					boolean isCopy = copyBtn.isSelected();
					GpxRootEntry destRoot = (destIdx < gpxFiles.size()) ? gpxFiles.get(destIdx) : null;
					mobac.data.gpx.gpx11.Gpx destGpx;
					GpxLayer destLayer;
					DefaultMutableTreeNode destNode;
					if (destRoot == null) {
						// Create new GPX file
						destGpx = mobac.data.gpx.gpx11.Gpx.createGpx();
						destLayer = new mobac.gui.mapview.layer.GpxLayer(destGpx);
						mobac.gui.panels.JGpxPanel panelRef = null;
						if (gpxFiles.size() > 0)
							panelRef = gpxFiles.get(0).getLayer().getPanel();
						if (panelRef == null)
							return;
						destRoot = panelRef.addGpxLayer(destLayer);
						destNode = destRoot.getNode();
						// Force tree to update and expand/select the new node before adding waypoints
						model.reload(destNode);
						model.nodeStructureChanged(destNode);
						gpxTree.expandPath(new javax.swing.tree.TreePath(destNode.getPath()));
						gpxTree.setSelectionPath(new javax.swing.tree.TreePath(destNode.getPath()));
					} else {
						destGpx = destRoot.getLayer().getGpx();
						destLayer = destRoot.getLayer();
						destNode = destRoot.getNode();
					}

					// For move, collect nodes to remove after dialog
					java.util.List<DefaultMutableTreeNode> nodesToRemove = new java.util.ArrayList<>();
					if (!isCopy) {
						nodesToRemove.addAll(selectedNodes);
					}
					// Clear destination waypoints before copying new ones
					destGpx.getWpt().clear();
					// Copy waypoints
					for (WptEntry entryToCopy : wptsToCopy) {
						mobac.data.gpx.gpx11.WptType orig = entryToCopy.getWpt();
						mobac.data.gpx.gpx11.WptType copy = new mobac.data.gpx.gpx11.WptType();
						copy.setName(orig.getName());
						copy.setLat(orig.getLat());
						copy.setLon(orig.getLon());
						copy.setEle(orig.getEle());
						copy.setTime(orig.getTime());
						copy.setDesc(orig.getDesc());
						copy.setCmt(orig.getCmt());
						copy.setSrc(orig.getSrc());
						copy.setSym(orig.getSym());
						copy.setType(orig.getType());
						copy.setFix(orig.getFix());
						copy.setSat(orig.getSat());
						copy.setHdop(orig.getHdop());
						copy.setVdop(orig.getVdop());
						copy.setPdop(orig.getPdop());
						copy.setAgeofdgpsdata(orig.getAgeofdgpsdata());
						copy.setDgpsid(orig.getDgpsid());
						destGpx.getWpt().add(copy);
						destRoot.setDirty(true);
					}
					// Always reload and expand the destination node to ensure waypoints are visible
					// Insert new waypoint nodes into the tree for all waypoints in destGpx
					destLayer.getPanel().addWpts(destLayer, destNode);
					model.reload(destNode);
					gpxTree.expandPath(new javax.swing.tree.TreePath(destNode.getPath()));
					// Remove original nodes if move
					if (!isCopy && !nodesToRemove.isEmpty()) {
						mobac.gui.panels.JGpxPanel.removeMultipleNodes(gpxTree, nodesToRemove);
					}
				});
				popup.add(copyMove);
			}
			// Always allow Delete for multi-selection
			JMenuItem delete = new JMenuItem(I18nUtils.localizedStringForKey("rp_gpx_pop_menu_delete_element"));
			delete.addActionListener(ev -> {
				java.util.List<javax.swing.tree.DefaultMutableTreeNode> nodesToDelete = new java.util.ArrayList<>();
				for (TreePath path : selectedPaths) {
					javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) path
							.getLastPathComponent();
					Object userObj = node.getUserObject();
					if (!(userObj instanceof mobac.gui.gpxtree.GpxRootEntry)) {
						nodesToDelete.add(node);
					}
				}
				if (nodesToDelete.isEmpty())
					return;
				int answer = javax.swing.JOptionPane.showConfirmDialog(null,
						I18nUtils.localizedStringForKey("rp_gpx_msg_confim_delete"),
						I18nUtils.localizedStringForKey("rp_gpx_msg_confim_delete_title"),
						javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
				if (answer != javax.swing.JOptionPane.YES_OPTION)
					return;
				tree.clearSelection();
				mobac.gui.panels.JGpxPanel.removeMultipleNodes(tree, nodesToDelete);
			});
			popup.add(delete);
			popup.show((Component) e.getSource(), e.getX(), e.getY());
			return;
		}
		// Single selection logic as before
		TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
		tree.setSelectionPath(selPath);
		if (selPath == null) {
			return;
		}

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();

		GpxEntry gpxEntry = null;
		try {
			gpxEntry = (GpxEntry) node.getUserObject();
			gpxEntry.setNode(node);
		} catch (ClassCastException exc) {
		}
		final GpxEntry gpxEntryFinal = gpxEntry;
		final DefaultMutableTreeNode nodeFinal = node;

		JPopupMenu popup = new JPopupMenu();
		GpxElementListener listener = new GpxElementListener(gpxEntry);

		// Show on map for waypoint entries
		if (gpxEntry instanceof WptEntry) {
			final WptEntry wptEntry = (WptEntry) gpxEntry;
			JMenuItem showOnMap = new JMenuItem(I18nUtils.localizedStringForKey("rp_gpx_menu_show_on_map"));
			showOnMap.addActionListener(ev -> {
				var wpt = wptEntry.getWpt();
				if (wpt != null && wpt.getLat() != null && wpt.getLon() != null) {
					double lat = wpt.getLat().doubleValue();
					double lon = wpt.getLon().doubleValue();
					int zoom = 15;
					mobac.gui.MainGUI.getMainGUI().previewMap.setDisplayPositionByLatLon(lat, lon, zoom);
				}
			});
			popup.add(showOnMap);

			// --- Properties/Edit dialog for waypoint ---
			JMenuItem properties = new JMenuItem(I18nUtils.localizedStringForKey("rp_gpx_menu_properties"));
			properties.addActionListener(ev -> {
				var wpt = wptEntry.getWpt();
				javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null,
						I18nUtils.localizedStringForKey("rp_gpx_dialog_waypoint_properties"), true);
				javax.swing.JPanel[] panelRef = new javax.swing.JPanel[1];
				// No shared gbc! Use a new one for each label/field
				// Initial panel
				panelRef[0] = new javax.swing.JPanel(new java.awt.GridBagLayout());

				// Field names and value suppliers
				String[] labels = {I18nUtils.localizedStringForKey("rp_gpx_label_name"),
						I18nUtils.localizedStringForKey("rp_gpx_label_latitude"),
						I18nUtils.localizedStringForKey("rp_gpx_label_longitude"),
						I18nUtils.localizedStringForKey("rp_gpx_label_elevation"),
						I18nUtils.localizedStringForKey("rp_gpx_label_time"),
						I18nUtils.localizedStringForKey("rp_gpx_label_description"),
						I18nUtils.localizedStringForKey("rp_gpx_label_comment"),
						I18nUtils.localizedStringForKey("rp_gpx_label_source"),
						I18nUtils.localizedStringForKey("rp_gpx_label_symbol"),
						I18nUtils.localizedStringForKey("rp_gpx_label_type"),
						I18nUtils.localizedStringForKey("rp_gpx_label_fix"),
						I18nUtils.localizedStringForKey("rp_gpx_label_satellites"),
						I18nUtils.localizedStringForKey("rp_gpx_label_hdop"),
						I18nUtils.localizedStringForKey("rp_gpx_label_vdop"),
						I18nUtils.localizedStringForKey("rp_gpx_label_pdop"),
						I18nUtils.localizedStringForKey("rp_gpx_label_ageofdgpsdata"),
						I18nUtils.localizedStringForKey("rp_gpx_label_dgpsid")};
				@SuppressWarnings("unchecked")
				java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>[] getters = (java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>[]) new java.util.function.Function[]{
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getName()),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getLat() != null
								? w.getLat().toPlainString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getLon() != null
								? w.getLon().toPlainString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getEle() != null
								? w.getEle().toPlainString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getTime() != null
								? w.getTime().toString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getDesc()),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getCmt()),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getSrc()),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getSym()),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getType()),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getFix()),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getSat() != null
								? w.getSat().toString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getHdop() != null
								? w.getHdop().toPlainString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getVdop() != null
								? w.getVdop().toPlainString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getPdop() != null
								? w.getPdop().toPlainString()
								: ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w
								.getAgeofdgpsdata() != null ? w.getAgeofdgpsdata().toPlainString() : ""),
						(java.util.function.Function<mobac.data.gpx.gpx11.WptType, String>) (w -> w.getDgpsid() != null
								? w.getDgpsid().toString()
								: "")};

				// Setters for edit mode
				@SuppressWarnings("unchecked")
				java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>[] setters = (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>[]) new java.util.function.BiConsumer[]{
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> w
								.setName(v.isEmpty() ? null : v),
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setLat(v.isEmpty() ? null : new java.math.BigDecimal(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setLon(v.isEmpty() ? null : new java.math.BigDecimal(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setEle(v.isEmpty() ? null : new java.math.BigDecimal(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								if (v.isEmpty())
									w.setTime(null);
								else
									w.setTime(javax.xml.datatype.DatatypeFactory.newInstance()
											.newXMLGregorianCalendar(v));
							} catch (Exception ex) {
							}
						},
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> w
								.setDesc(v.isEmpty() ? null : v),
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> w
								.setCmt(v.isEmpty() ? null : v),
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> w
								.setSrc(v.isEmpty() ? null : v),
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> w
								.setSym(v.isEmpty() ? null : v),
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> w
								.setType(v.isEmpty() ? null : v),
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> w
								.setFix(v.isEmpty() ? null : v),
						(java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setSat(v.isEmpty() ? null : new java.math.BigInteger(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setHdop(v.isEmpty() ? null : new java.math.BigDecimal(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setVdop(v.isEmpty() ? null : new java.math.BigDecimal(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setPdop(v.isEmpty() ? null : new java.math.BigDecimal(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setAgeofdgpsdata(v.isEmpty() ? null : new java.math.BigDecimal(v));
							} catch (Exception ex) {
							}
						}, (java.util.function.BiConsumer<mobac.data.gpx.gpx11.WptType, String>) (w, v) -> {
							try {
								w.setDgpsid(v.isEmpty() ? null : Integer.valueOf(v));
							} catch (Exception ex) {
							}
						}};

				// In view mode, only show fields with non-empty values
				java.util.List<Integer> visibleFieldIndexes = new java.util.ArrayList<>();
				for (int i = 0; i < labels.length; i++) {
					String value = getters[i].apply(wpt);
					if (value != null && !value.isEmpty()) {
						visibleFieldIndexes.add(i);
					}
				}
				final javax.swing.JTextField[][] fieldsRef = new javax.swing.JTextField[1][];
				fieldsRef[0] = new javax.swing.JTextField[labels.length];

				// Button panel and buttons must be declared before lambdas
				javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
				javax.swing.JButton editBtn = new javax.swing.JButton(
						I18nUtils.localizedStringForKey("rp_gpx_btn_edit"));
				javax.swing.JButton saveBtn = new javax.swing.JButton(
						I18nUtils.localizedStringForKey("rp_gpx_btn_save"));
				javax.swing.JButton cancelBtn = new javax.swing.JButton(
						I18nUtils.localizedStringForKey("rp_gpx_btn_cancel"));
				saveBtn.setVisible(false);
				cancelBtn.setVisible(false);
				buttonPanel.add(editBtn);
				buttonPanel.add(saveBtn);
				buttonPanel.add(cancelBtn);

				// Helper to rebuild panel for edit mode (show all fields)
				Runnable showAllFields = () -> {
					javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
					javax.swing.JTextField[] fields = new javax.swing.JTextField[labels.length];
					for (int i = 0; i < labels.length; i++) {
						java.awt.GridBagConstraints gbcLabel = new java.awt.GridBagConstraints();
						gbcLabel.gridx = 0;
						gbcLabel.gridy = i;
						gbcLabel.insets = new java.awt.Insets(2, 4, 2, 4);
						gbcLabel.anchor = java.awt.GridBagConstraints.WEST;
						gbcLabel.fill = java.awt.GridBagConstraints.NONE;
						gbcLabel.weightx = 0;
						panel.add(new javax.swing.JLabel(labels[i]), gbcLabel);

						java.awt.GridBagConstraints gbcField = new java.awt.GridBagConstraints();
						gbcField.gridx = 1;
						gbcField.gridy = i;
						gbcField.insets = new java.awt.Insets(2, 4, 2, 4);
						gbcField.anchor = java.awt.GridBagConstraints.WEST;
						gbcField.fill = java.awt.GridBagConstraints.HORIZONTAL;
						gbcField.weightx = 1.0;
						fields[i] = new javax.swing.JTextField(getters[i].apply(wpt));
						fields[i].setEditable(true);
						panel.add(fields[i], gbcField);
					}
					fieldsRef[0] = fields;
					java.awt.GridBagConstraints gbcBtn = new java.awt.GridBagConstraints();
					gbcBtn.gridx = 0;
					gbcBtn.gridy = labels.length;
					gbcBtn.gridwidth = 2;
					gbcBtn.insets = new java.awt.Insets(2, 4, 2, 4);
					gbcBtn.anchor = java.awt.GridBagConstraints.CENTER;
					panel.add(buttonPanel, gbcBtn);
					panelRef[0] = panel;
					dialog.setContentPane(panel);
					panel.revalidate();
					panel.repaint();
					dialog.pack();
				};

				// Helper to rebuild panel for view mode (show only non-empty fields, but always
				// show Edit button)
				Runnable showNonEmptyFields = () -> {
					javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
					javax.swing.JTextField[] fields = new javax.swing.JTextField[labels.length];
					int row2 = 0;
					for (int idx : visibleFieldIndexes) {
						java.awt.GridBagConstraints gbcLabel = new java.awt.GridBagConstraints();
						gbcLabel.gridx = 0;
						gbcLabel.gridy = row2;
						gbcLabel.insets = new java.awt.Insets(2, 4, 2, 4);
						gbcLabel.anchor = java.awt.GridBagConstraints.WEST;
						gbcLabel.fill = java.awt.GridBagConstraints.NONE;
						gbcLabel.weightx = 0;
						panel.add(new javax.swing.JLabel(labels[idx]), gbcLabel);

						java.awt.GridBagConstraints gbcField = new java.awt.GridBagConstraints();
						gbcField.gridx = 1;
						gbcField.gridy = row2;
						gbcField.insets = new java.awt.Insets(2, 4, 2, 4);
						gbcField.anchor = java.awt.GridBagConstraints.WEST;
						gbcField.fill = java.awt.GridBagConstraints.HORIZONTAL;
						gbcField.weightx = 1.0;
						fields[idx] = new javax.swing.JTextField(getters[idx].apply(wpt));
						fields[idx].setEditable(false);
						panel.add(fields[idx], gbcField);
						row2++;
					}
					fieldsRef[0] = fields;
					// Always show the button panel at the bottom
					java.awt.GridBagConstraints gbcBtn = new java.awt.GridBagConstraints();
					gbcBtn.gridx = 0;
					gbcBtn.gridy = Math.max(row2, 1);
					gbcBtn.gridwidth = 2;
					gbcBtn.insets = new java.awt.Insets(2, 4, 2, 4);
					gbcBtn.anchor = java.awt.GridBagConstraints.CENTER;
					panel.add(buttonPanel, gbcBtn);
					panelRef[0] = panel;
					dialog.setContentPane(panel);
					panel.revalidate();
					panel.repaint();
					dialog.pack();
				};

				// Initially, only add non-empty fields and always add button panel at the
				// bottom
				// Initial view mode layout
				int row = 0;
				javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
				javax.swing.JTextField[] fields = new javax.swing.JTextField[labels.length];
				for (int idx : visibleFieldIndexes) {
					java.awt.GridBagConstraints gbcLabel = new java.awt.GridBagConstraints();
					gbcLabel.gridx = 0;
					gbcLabel.gridy = row;
					gbcLabel.insets = new java.awt.Insets(2, 4, 2, 4);
					gbcLabel.anchor = java.awt.GridBagConstraints.WEST;
					gbcLabel.fill = java.awt.GridBagConstraints.NONE;
					gbcLabel.weightx = 0;
					panel.add(new javax.swing.JLabel(labels[idx]), gbcLabel);

					java.awt.GridBagConstraints gbcField = new java.awt.GridBagConstraints();
					gbcField.gridx = 1;
					gbcField.gridy = row;
					gbcField.insets = new java.awt.Insets(2, 4, 2, 4);
					gbcField.anchor = java.awt.GridBagConstraints.WEST;
					gbcField.fill = java.awt.GridBagConstraints.HORIZONTAL;
					gbcField.weightx = 1.0;
					fields[idx] = new javax.swing.JTextField(getters[idx].apply(wpt));
					fields[idx].setEditable(false);
					panel.add(fields[idx], gbcField);
					row++;
				}
				fieldsRef[0] = fields;
				java.awt.GridBagConstraints gbcBtn = new java.awt.GridBagConstraints();
				gbcBtn.gridx = 0;
				gbcBtn.gridy = Math.max(row, 1);
				gbcBtn.gridwidth = 2;
				gbcBtn.insets = new java.awt.Insets(2, 4, 2, 4);
				gbcBtn.anchor = java.awt.GridBagConstraints.CENTER;
				panel.add(buttonPanel, gbcBtn);
				panelRef[0] = panel;
				dialog.setContentPane(panel);
				panel.revalidate();
				panel.repaint();
				dialog.pack();

				// Edit mode logic
				editBtn.addActionListener(ev2 -> {
					editBtn.setVisible(false);
					saveBtn.setVisible(true);
					cancelBtn.setVisible(true);
					showAllFields.run();
				});
				cancelBtn.addActionListener(ev2 -> {
					editBtn.setVisible(true);
					saveBtn.setVisible(false);
					cancelBtn.setVisible(false);
					showNonEmptyFields.run();
				});
				saveBtn.addActionListener(ev2 -> {
					for (int i = 0; i < fieldsRef[0].length; i++)
						if (fieldsRef[0][i] != null)
							setters[i].accept(wpt, fieldsRef[0][i].getText());
					// Recompute visibleFieldIndexes after save
					visibleFieldIndexes.clear();
					for (int i = 0; i < labels.length; i++) {
						String value = getters[i].apply(wpt);
						if (value != null && !value.isEmpty()) {
							visibleFieldIndexes.add(i);
						}
					}
					editBtn.setVisible(true);
					saveBtn.setVisible(false);
					cancelBtn.setVisible(false);
					// Mark parent GPX file as dirty and update its node in the tree
					javax.swing.tree.TreeNode parentNode = wptEntry.getNode();
					javax.swing.tree.DefaultMutableTreeNode gpxFileNode = null;
					while (parentNode != null) {
						Object userObj = ((javax.swing.tree.DefaultMutableTreeNode) parentNode).getUserObject();
						if (userObj instanceof mobac.gui.gpxtree.GpxRootEntry) {
							((mobac.gui.gpxtree.GpxRootEntry) userObj).setDirty(true);
							gpxFileNode = (javax.swing.tree.DefaultMutableTreeNode) parentNode;
							break;
						}
						parentNode = ((javax.swing.tree.DefaultMutableTreeNode) parentNode).getParent();
					}
					javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) ((JTree) tree)
							.getModel();
					// Update both the waypoint node and the GPX file node
					if (wptEntry.getNode() != null) {
						model.nodeChanged(wptEntry.getNode());
					}
					if (gpxFileNode != null) {
						model.nodeChanged(gpxFileNode);
					}
					// Repaint map to update waypoint icon position and label
					if (wptEntry.getLayer() != null && wptEntry.getLayer().getPanel() != null
							&& wptEntry.getLayer().getPanel().getPreviewMap() != null) {
						wptEntry.getLayer().getPanel().getPreviewMap().repaint();
					}
					// After save, show only non-empty fields
					showNonEmptyFields.run();
				});

				dialog.setContentPane(panel);
				dialog.pack();
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
			});
			popup.add(properties);

			// --- Copy/Move dialog for waypoint(s) ---
			JMenuItem copyMove = new JMenuItem("Copy/Move..."); // TODO: localize
			copyMove.addActionListener(ev -> {
				javax.swing.JTree gpxTree = (javax.swing.JTree) ((javax.swing.JPopupMenu) popup).getInvoker();
				javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) gpxTree.getModel();
				javax.swing.tree.DefaultMutableTreeNode root = (javax.swing.tree.DefaultMutableTreeNode) model
						.getRoot();
				java.util.List<GpxRootEntry> gpxFiles = new java.util.ArrayList<>();
				for (int i = 0; i < root.getChildCount(); i++) {
					Object userObj = ((javax.swing.tree.DefaultMutableTreeNode) root.getChildAt(i)).getUserObject();
					if (userObj instanceof GpxRootEntry) {
						gpxFiles.add((GpxRootEntry) userObj);
					}
				}
				String[] gpxNames = new String[gpxFiles.size() + 1];
				for (int i = 0; i < gpxFiles.size(); i++) {
					gpxNames[i] = gpxFiles.get(i).toString();
				}
				gpxNames[gpxFiles.size()] = "<New GPX file>";

				// --- Collect selected waypoints BEFORE any tree selection changes ---
				javax.swing.tree.TreePath[] selPaths = gpxTree.getSelectionPaths();
				if (selPaths == null)
					return;
				java.util.List<WptEntry> wptsToCopy = new java.util.ArrayList<>();
				java.util.List<DefaultMutableTreeNode> selectedNodes = new java.util.ArrayList<>();
				for (javax.swing.tree.TreePath path : selPaths) {
					Object userObj = ((javax.swing.tree.DefaultMutableTreeNode) path.getLastPathComponent())
							.getUserObject();
					if (userObj instanceof WptEntry) {
						wptsToCopy.add((WptEntry) userObj);
						selectedNodes.add((DefaultMutableTreeNode) path.getLastPathComponent());
					}
				}
				if (wptsToCopy.isEmpty())
					return;

				javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(0, 1));
				javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
				javax.swing.JRadioButton copyBtn = new javax.swing.JRadioButton("Copy", true);
				javax.swing.JRadioButton moveBtn = new javax.swing.JRadioButton("Move");
				group.add(copyBtn);
				group.add(moveBtn);
				panel.add(copyBtn);
				panel.add(moveBtn);
				panel.add(new javax.swing.JLabel("Destination GPX file:"));
				javax.swing.JComboBox<String> gpxCombo = new javax.swing.JComboBox<>(gpxNames);
				panel.add(gpxCombo);

				int result = javax.swing.JOptionPane.showConfirmDialog(null, panel, "Copy/Move Waypoint(s)",
						javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
				if (result != javax.swing.JOptionPane.OK_OPTION)
					return;

				int destIdx = gpxCombo.getSelectedIndex();
				boolean isCopy = copyBtn.isSelected();
				GpxRootEntry destRoot = (destIdx < gpxFiles.size()) ? gpxFiles.get(destIdx) : null;
				mobac.data.gpx.gpx11.Gpx destGpx;
				GpxLayer destLayer;
				DefaultMutableTreeNode destNode;
				if (destRoot == null) {
					// Create new GPX file
					destGpx = mobac.data.gpx.gpx11.Gpx.createGpx();
					destLayer = new mobac.gui.mapview.layer.GpxLayer(destGpx);
					mobac.gui.panels.JGpxPanel panelRef = null;
					if (gpxFiles.size() > 0)
						panelRef = gpxFiles.get(0).getLayer().getPanel();
					if (panelRef == null)
						return;
					destRoot = panelRef.addGpxLayer(destLayer);
					destNode = destRoot.getNode();
					// Force tree to update and expand/select the new node before adding waypoints
					model.reload(destNode);
					gpxTree.expandPath(new javax.swing.tree.TreePath(destNode.getPath()));
					gpxTree.setSelectionPath(new javax.swing.tree.TreePath(destNode.getPath()));
				} else {
					destGpx = destRoot.getLayer().getGpx();
					destLayer = destRoot.getLayer();
					destNode = destRoot.getNode();
				}

				// For move, collect nodes to remove after dialog
				java.util.List<DefaultMutableTreeNode> nodesToRemove = new java.util.ArrayList<>();
				if (!isCopy) {
					nodesToRemove.addAll(selectedNodes);
				}
				// Copy waypoints
				for (WptEntry entryToCopy : wptsToCopy) {
					mobac.data.gpx.gpx11.WptType orig = entryToCopy.getWpt();
					mobac.data.gpx.gpx11.WptType copy = new mobac.data.gpx.gpx11.WptType();
					copy.setName(orig.getName());
					copy.setLat(orig.getLat());
					copy.setLon(orig.getLon());
					copy.setEle(orig.getEle());
					copy.setTime(orig.getTime());
					copy.setDesc(orig.getDesc());
					copy.setCmt(orig.getCmt());
					copy.setSrc(orig.getSrc());
					copy.setSym(orig.getSym());
					copy.setType(orig.getType());
					copy.setFix(orig.getFix());
					copy.setSat(orig.getSat());
					copy.setHdop(orig.getHdop());
					copy.setVdop(orig.getVdop());
					copy.setPdop(orig.getPdop());
					copy.setAgeofdgpsdata(orig.getAgeofdgpsdata());
					copy.setDgpsid(orig.getDgpsid());
					destGpx.getWpt().add(copy);
				}
				// Ensure all waypoints in destGpx are shown in the tree
				// Remove all existing waypoint nodes from destNode before re-adding
				javax.swing.tree.DefaultTreeModel destModel = (javax.swing.tree.DefaultTreeModel) gpxTree.getModel();
				java.util.List<javax.swing.tree.DefaultMutableTreeNode> nodesToRemoveFromDest = new java.util.ArrayList<>();
				for (int i = 0; i < destNode.getChildCount(); i++) {
					javax.swing.tree.DefaultMutableTreeNode child = (javax.swing.tree.DefaultMutableTreeNode) destNode
							.getChildAt(i);
					Object userObj = child.getUserObject();
					if (userObj instanceof mobac.gui.gpxtree.WptEntry) {
						nodesToRemoveFromDest.add(child);
					}
				}
				for (javax.swing.tree.DefaultMutableTreeNode nodeToRemove : nodesToRemoveFromDest) {
					destModel.removeNodeFromParent(nodeToRemove);
				}
				destLayer.getPanel().addWpts(destLayer, destNode);
				destRoot.setDirty(true);
				model.nodeChanged(destNode);
				// Always reload and expand the destination node to ensure waypoints are visible
				model.reload(destNode);
				gpxTree.expandPath(new javax.swing.tree.TreePath(destNode.getPath()));
				gpxTree.setSelectionPath(new javax.swing.tree.TreePath(destNode.getPath()));
				if (!isCopy && !nodesToRemove.isEmpty()) {
					mobac.gui.panels.JGpxPanel.removeMultipleNodes(gpxTree, nodesToRemove);
				}
			});
			popup.add(copyMove);
		}

		// Show on map for GPX file (root) entries
		if (gpxEntry instanceof GpxRootEntry) {
			final GpxRootEntry rootEntry = (GpxRootEntry) gpxEntry;
			JMenuItem showOnMap = new JMenuItem("Show on map");
			showOnMap.addActionListener(ev -> {
				var gpx = rootEntry.getLayer().getGpx();
				// Collect all valid points
				java.util.List<Double[]> validPoints = new java.util.ArrayList<>();
				for (var wpt : gpx.getWpt()) {
					if (wpt.getLat() != null && wpt.getLon() != null) {
						validPoints.add(new Double[]{wpt.getLat().doubleValue(), wpt.getLon().doubleValue()});
					}
				}
				for (var rte : gpx.getRte()) {
					for (var rtept : rte.getRtept()) {
						if (rtept.getLat() != null && rtept.getLon() != null) {
							validPoints.add(new Double[]{rtept.getLat().doubleValue(), rtept.getLon().doubleValue()});
						}
					}
				}
				for (var trk : gpx.getTrk()) {
					for (var seg : trk.getTrkseg()) {
						for (var trkpt : seg.getTrkpt()) {
							if (trkpt.getLat() != null && trkpt.getLon() != null) {
								validPoints
										.add(new Double[]{trkpt.getLat().doubleValue(), trkpt.getLon().doubleValue()});
							}
						}
					}
				}
				if (validPoints.isEmpty()) {
					javax.swing.JOptionPane.showMessageDialog(null, "No valid coordinates found in this GPX file.",
							"Show on map", javax.swing.JOptionPane.WARNING_MESSAGE);
					return;
				}
				if (validPoints.size() == 1) {
					Double[] pt = validPoints.get(0);
					mobac.gui.MainGUI.getMainGUI().previewMap.setDisplayPositionByLatLon(pt[0], pt[1], 15);
					return;
				}
				// Compute bounds
				double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
				double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
				for (Double[] pt : validPoints) {
					minLat = Math.min(minLat, pt[0]);
					maxLat = Math.max(maxLat, pt[0]);
					minLon = Math.min(minLon, pt[1]);
					maxLon = Math.max(maxLon, pt[1]);
				}
				var map = mobac.gui.MainGUI.getMainGUI().previewMap;
				var mapSource = map.getMapSource();
				var mapSpace = mapSource.getMapSpace();
				int mapWidth = map.getWidth();
				int mapHeight = map.getHeight();
				double centerLat = (minLat + maxLat) / 2.0;
				double centerLon = (minLon + maxLon) / 2.0;
				int bestZoom = mapSource.getMaxZoom();
				if (minLat == maxLat && minLon == maxLon) {
					// All points are identical
					map.setDisplayPositionByLatLon(minLat, minLon, 15);
				} else if (minLat == maxLat) {
					// All points share latitude, vary longitude
					for (int zoom = mapSource.getMaxZoom(); zoom >= mapSource.getMinZoom(); zoom--) {
						int x1 = mapSpace.cLonToX(minLon, zoom);
						int x2 = mapSpace.cLonToX(maxLon, zoom);
						int boxWidth = Math.abs(x2 - x1);
						if (boxWidth <= mapWidth * 0.9) {
							bestZoom = zoom;
							break;
						}
					}
					map.setDisplayPositionByLatLon(centerLat, centerLon, bestZoom);
				} else if (minLon == maxLon) {
					// All points share longitude, vary latitude
					for (int zoom = mapSource.getMaxZoom(); zoom >= mapSource.getMinZoom(); zoom--) {
						int y1 = mapSpace.cLatToY(maxLat, zoom);
						int y2 = mapSpace.cLatToY(minLat, zoom);
						int boxHeight = Math.abs(y2 - y1);
						if (boxHeight <= mapHeight * 0.9) {
							bestZoom = zoom;
							break;
						}
					}
					map.setDisplayPositionByLatLon(centerLat, centerLon, bestZoom);
				} else {
					centerMapToBounds(new double[]{minLat, maxLat, minLon, maxLon});
				}
			});
			popup.add(showOnMap);
		}

		// Show on map for track (TrkEntry) nodes
		if (gpxEntry instanceof TrkEntry) {
			final TrkEntry trkEntry = (TrkEntry) gpxEntry;
			JMenuItem showOnMap = new JMenuItem("Show on map");
			showOnMap.addActionListener(ev -> {
				var trk = trkEntry.getTrk();
				double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
				double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
				for (var seg : trk.getTrkseg()) {
					for (var trkpt : seg.getTrkpt()) {
						if (trkpt.getLat() != null && trkpt.getLon() != null) {
							double lat = trkpt.getLat().doubleValue();
							double lon = trkpt.getLon().doubleValue();
							minLat = Math.min(minLat, lat);
							maxLat = Math.max(maxLat, lat);
							minLon = Math.min(minLon, lon);
							maxLon = Math.max(maxLon, lon);
						}
					}
				}
				if (minLat < maxLat && minLon < maxLon) {
					centerMapToBounds(new double[]{minLat, maxLat, minLon, maxLon});
				}
			});
			popup.add(showOnMap);
		}

		// Show on map for route (RteEntry) nodes
		if (gpxEntry instanceof RteEntry) {
			final RteEntry rteEntry = (RteEntry) gpxEntry;
			JMenuItem showOnMap = new JMenuItem("Show on map");
			showOnMap.addActionListener(ev -> {
				var rte = rteEntry.getRte();
				double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
				double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
				for (var rtept : rte.getRtept()) {
					if (rtept.getLat() != null && rtept.getLon() != null) {
						double lat = rtept.getLat().doubleValue();
						double lon = rtept.getLon().doubleValue();
						minLat = Math.min(minLat, lat);
						maxLat = Math.max(maxLat, lat);
						minLon = Math.min(minLon, lon);
						maxLon = Math.max(maxLon, lon);
					}
				}
				if (minLat < maxLat && minLon < maxLon) {
					centerMapToBounds(new double[]{minLat, maxLat, minLon, maxLon});
				}
			});
			popup.add(showOnMap);
		}

		// For GPX file (root) nodes, replace 'delete' with 'close file' and handle
		// dirty state
		if (gpxEntry instanceof GpxRootEntry) {
			final GpxRootEntry rootEntry = (GpxRootEntry) gpxEntry;
			// Show real filename if available
			java.io.File file = rootEntry.getLayer() != null ? rootEntry.getLayer().getFile() : null;
			if (file != null) {
				JMenuItem fileNameItem = new JMenuItem("File: " + file.getName());
				fileNameItem.setEnabled(false);
				popup.add(fileNameItem);
			}

			JMenuItem moveUp = new JMenuItem("Move Up");
			moveUp.addActionListener(ev -> {
				JTree treeComponent = (JTree) e.getSource();
				// Save expanded paths
				java.util.Enumeration<TreePath> expanded = treeComponent
						.getExpandedDescendants(new TreePath(treeComponent.getModel().getRoot()));
				java.util.List<TreePath> expandedPaths = new java.util.ArrayList<>();
				if (expanded != null) {
					while (expanded.hasMoreElements()) {
						expandedPaths.add(expanded.nextElement());
					}
				}
				DefaultMutableTreeNode nodeToMove = rootEntry.getNode();
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) nodeToMove.getParent();
				int idx = parent.getIndex(nodeToMove);
				if (idx > 0) {
					javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) treeComponent
							.getModel();
					parent.insert(nodeToMove, idx - 1);
					model.nodeStructureChanged(parent);
					treeComponent.setSelectionPath(new TreePath(nodeToMove.getPath()));
					// Restore expanded paths
					for (TreePath path : expandedPaths) {
						treeComponent.expandPath(path);
					}
				}
			});
			popup.add(moveUp);

			JMenuItem moveDown = new JMenuItem("Move Down");
			moveDown.addActionListener(ev -> {
				JTree treeComponent = (JTree) e.getSource();
				// Save expanded paths
				java.util.Enumeration<TreePath> expanded = treeComponent
						.getExpandedDescendants(new TreePath(treeComponent.getModel().getRoot()));
				java.util.List<TreePath> expandedPaths = new java.util.ArrayList<>();
				if (expanded != null) {
					while (expanded.hasMoreElements()) {
						expandedPaths.add(expanded.nextElement());
					}
				}
				DefaultMutableTreeNode nodeToMove = rootEntry.getNode();
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) nodeToMove.getParent();
				int idx = parent.getIndex(nodeToMove);
				if (idx < parent.getChildCount() - 1) {
					javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) treeComponent
							.getModel();
					parent.insert(nodeToMove, idx + 1);
					model.nodeStructureChanged(parent);
					treeComponent.setSelectionPath(new TreePath(nodeToMove.getPath()));
					// Restore expanded paths
					for (TreePath path : expandedPaths) {
						treeComponent.expandPath(path);
					}
				}
			});
			popup.add(moveDown);

			JMenuItem saveFile = new JMenuItem("Save");
			boolean hasFile = rootEntry.getLayer() != null && rootEntry.getLayer().getFile() != null;
			saveFile.setEnabled(rootEntry.isDirty() && hasFile);
			saveFile.addActionListener(ev -> {
				if (!rootEntry.isDirty())
					return;
				String filePath = null;
				if (rootEntry.getLayer() != null && rootEntry.getLayer().getFile() != null) {
					filePath = rootEntry.getLayer().getFile().getAbsolutePath();
				}
				String msg = "Saving will overwrite the file and deleted data will be lost.\n";
				if (filePath != null) {
					msg += "\nFile: " + filePath;
				}
				int result = javax.swing.JOptionPane.showConfirmDialog(null, msg, "Confirm Save",
						javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
				if (result != javax.swing.JOptionPane.YES_OPTION)
					return;
				if (rootEntry.getLayer() != null && rootEntry.getLayer().getPanel() != null) {
					mobac.gui.actions.GpxSave saveAction = new mobac.gui.actions.GpxSave(
							rootEntry.getLayer().getPanel());
					saveAction.actionPerformed(null);
				}
			});
			popup.add(saveFile);

			JMenuItem saveAsFile = new JMenuItem("Save as...");
			saveAsFile.addActionListener(ev -> {
				if (rootEntry.getLayer() != null && rootEntry.getLayer().getPanel() != null) {
					// Always force Save As dialog
					mobac.gui.actions.GpxSave saveAsAction = new mobac.gui.actions.GpxSave(
							rootEntry.getLayer().getPanel(), true);
					saveAsAction.actionPerformed(null);
				}
			});
			popup.add(saveAsFile);

			JMenuItem closeFile = new JMenuItem("Close file");
			closeFile.addActionListener(ev -> {
				boolean isDirty = rootEntry.isDirty();
				boolean proceed = false;
				if (!isDirty) {
					proceed = true;
				} else {
					String message = "This file has unsaved changes. Close anyway and lose changes?";
					int optionType = javax.swing.JOptionPane.YES_NO_OPTION;
					int messageType = javax.swing.JOptionPane.WARNING_MESSAGE;
					int result = javax.swing.JOptionPane.showConfirmDialog(null, message, "Unsaved Changes", optionType,
							messageType);
					proceed = (result == javax.swing.JOptionPane.YES_OPTION);
				}
				if (proceed) {
					// Remove the node from the tree and the layer from the map
					DefaultMutableTreeNode nodeToRemove = rootEntry.getNode();
					if (nodeToRemove != null) {
						javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) ((JTree) e
								.getSource()).getModel();
						model.removeNodeFromParent(nodeToRemove);
					}
					if (rootEntry.getLayer() != null && rootEntry.getLayer().getPanel() != null) {
						var panel = rootEntry.getLayer().getPanel();
						panel.getPreviewMap().mapLayers.remove(rootEntry.getLayer());
						panel.getPreviewMap().repaint();
						// Remove from openedFiles if file exists
						if (file != null) {
							panel.removeOpenedFile(file.getAbsolutePath());
						}
					}
				}
			});
			popup.add(closeFile);
		} else {
			JMenuItem delete = new JMenuItem(I18nUtils.localizedStringForKey("rp_gpx_pop_menu_delete_element"));
			delete.setName(GpxElementListener.MENU_NAME_DELETE);
			delete.addActionListener(ev -> {
				// Inline logic: mimic GpxElementListener's removeEntry for single node
				if (gpxEntryFinal instanceof mobac.gui.gpxtree.WptEntry) {
					mobac.gui.gpxtree.WptEntry wptEntry = (mobac.gui.gpxtree.WptEntry) gpxEntryFinal;
					int answer = javax.swing.JOptionPane.showConfirmDialog(null,
							I18nUtils.localizedStringForKey("rp_gpx_msg_confim_delete"),
							I18nUtils.localizedStringForKey("rp_gpx_msg_confim_delete_title"),
							javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
					if (answer == javax.swing.JOptionPane.YES_OPTION) {
						wptEntry.getLayer().getPanel().removeWpt(wptEntry);
					}
				} else if (gpxEntryFinal instanceof mobac.gui.gpxtree.TrkEntry
						|| gpxEntryFinal instanceof mobac.gui.gpxtree.TrksegEntry
						|| gpxEntryFinal instanceof mobac.gui.gpxtree.RteEntry) {
					javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) tree.getModel();
					int answer = javax.swing.JOptionPane.showConfirmDialog(null,
							I18nUtils.localizedStringForKey("rp_gpx_msg_confim_delete"),
							I18nUtils.localizedStringForKey("rp_gpx_msg_confim_delete_title"),
							javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
					if (answer == javax.swing.JOptionPane.YES_OPTION) {
						model.removeNodeFromParent(nodeFinal);
					}
				}
			});
			popup.add(delete);
		}
		JMenuItem rename = new JMenuItem("Rename");
		rename.setName(GpxElementListener.MENU_NAME_RENAME);
		rename.addMouseListener(listener);
		popup.add(rename);

		popup.show((Component) e.getSource(), e.getX(), e.getY());
	}
}
