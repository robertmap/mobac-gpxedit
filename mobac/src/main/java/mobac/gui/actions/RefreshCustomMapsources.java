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

import jakarta.xml.bind.JAXBException;
import mobac.exceptions.MapSourceCreateException;
import mobac.exceptions.MapSourceInitializationException;
import mobac.gui.MainGUI;
import mobac.mapsources.MapSourcesManager;
import mobac.mapsources.loader.CustomMapSourceLoader;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.MapSourceLoaderInfo.LoaderType;
import mobac.program.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RefreshCustomMapsources implements ActionListener {

	private static final String[] COL_NAMES = new String[]{"Map name", "File path", "Status", "Restart required"};
	private final Logger log = LoggerFactory.getLogger(RefreshCustomMapsources.class);

	public static void main(String[] args) {
		List<ReloadTableEntry> list = new ArrayList<>();
		list.add(new ReloadTableEntry("name", "path", "status", false));
		ReloadInfoDialog dialog = new ReloadInfoDialog(list);
		dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void actionPerformed(ActionEvent event) {
		MapSourcesManager manager = MapSourcesManager.getInstance();
		MainGUI gui = MainGUI.getMainGUI();
		MapSource selectedMapSource = gui.getSelectedMapSource();
		boolean updateGui = false;
		int count = 0;

		List<ReloadTableEntry> entries = new ArrayList<>();
		File mapSourcesDir = Settings.getInstance().getMapSourcesDirectory();
		Path mapSourcesDirPath = mapSourcesDir.toPath();
		CustomMapSourceLoader cmsl = new CustomMapSourceLoader(manager, mapSourcesDir);
		List<File> newFileList = cmsl.getMapSourceFiles();

		Set<File> addedFiles = new HashSet<>(newFileList);
		for (MapSource ms : manager.getAllAvailableMapSources()) {
			if ((ms.getLoaderInfo() == null) || (ms.getLoaderInfo().getSourceFile() == null)) {
				continue;
			}
			addedFiles.remove(ms.getLoaderInfo().getSourceFile());
		}

		for (final MapSource mapSource : manager.getAllAvailableMapSources()) {
			String relPath = "";
			if (mapSource.getLoaderInfo() != null) {
				MapSourceLoaderInfo loaderInfo = mapSource.getLoaderInfo();
				if (loaderInfo.getLoaderType() == LoaderType.MAPPACK) {
					continue; // skip map sources from map packs
				}
				if (loaderInfo.getSourceFile() != null) {
					relPath = mapSourcesDirPath.relativize(loaderInfo.getSourceFile().toPath()).toString();
				}
			}
			try {
				boolean reloaded = false;
				boolean dataRefreshed = false;
				try {
					reloaded = cmsl.reloadCustomMapSource(mapSource);
				} catch (MapSourceCreateException | JAXBException | IOException | SAXException
						| MapSourceInitializationException e) {
					log.error("Failed to reload map source: " + e.getMessage(), e);
				}
				if (mapSource instanceof FileBasedMapSource) {
					FileBasedMapSource fbms = (FileBasedMapSource) mapSource;
					fbms.reinitialize();
					dataRefreshed = true;
				}
				if (reloaded || dataRefreshed) {
					count++;
					if (mapSource.equals(selectedMapSource)) {
						updateGui = true;
					}
				}
				String status = "";
				if (reloaded) {
					status = "reloaded";
				}
				if (dataRefreshed) {
					if (!status.isEmpty()) {
						status += " & ";
					}
					status += "data refreshed";
				}
				if (status.isEmpty()) {
					status = "unchanged";
				}
				entries.add(new ReloadTableEntry(mapSource.getName(), relPath, status, false));
			} catch (Exception e) {
				entries.add(new ReloadTableEntry(mapSource.getName(), relPath, "Reloading failed: " + e.getMessage(),
						true));
			}
		}
		for (File f : addedFiles) {
			String relPath = mapSourcesDirPath.relativize(f.toPath()).toString();
			try {
				MapSource addedMapSource = cmsl.loadCustomMapSource(f);
				if (addedMapSource != null) {
					entries.add(new ReloadTableEntry(addedMapSource.getName(), relPath, "New file", false));
					manager.addMapSource(addedMapSource);
					updateGui = true;
				}
			} catch (Exception e) {
				log.error("Failed to load map source " + f, e);
				entries.add(new ReloadTableEntry("?", relPath, "New file", true));
			}
		}

		Collections.sort(entries);
		new ReloadInfoDialog(entries);

		if (updateGui) {
			/*
			 * The currently selected map source was updated - we have to force an GUI
			 * update in case the available zoom levels has been changed
			 */
			gui.updateMapSourcesList();
			gui.mapSourceChanged(selectedMapSource);
		}
		// JOptionPane.showMessageDialog(gui,
		// String.format(I18nUtils.localizedStringForKey("msg_refresh_all_map_source_done"),
		// count));
	}

	private static class ReloadInfoDialog extends JFrame {

		public ReloadInfoDialog(List<ReloadTableEntry> entries) throws HeadlessException {
			super("Reloaded map sources");
			final JTable table = new JTable(new ReloadTableModel(entries));
			table.setFillsViewportHeight(true);
			// table.setPreferredScrollableViewportSize(new Dimension(500, 70));
			// table.setFillsViewportHeight(true);
			// setLayout(new BorderLayout());

			JScrollPane scrollPane = new JScrollPane(table);
			add(scrollPane);

			pack();
			setVisible(true);
		}

	}

	private static class ReloadTableModel extends AbstractTableModel {

		final List<ReloadTableEntry> entries;

		public ReloadTableModel(List<ReloadTableEntry> entries) {
			super();
			this.entries = entries;
		}

		@Override
		public int getRowCount() {
			return entries.size();
		}

		@Override
		public int getColumnCount() {
			return COL_NAMES.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			ReloadTableEntry entry = entries.get(rowIndex);
			switch (columnIndex) {
				case 0 :
					return entry.name;
				case 1 :
					return entry.relativeFilePath;
				case 2 :
					return entry.status;
				case 3 :
					return entry.restartRequired ? "yes" : "no";
			}
			return null;
		}

		@Override
		public String getColumnName(int column) {
			return COL_NAMES[column];
		}

	}

	private static class ReloadTableEntry implements Comparable<ReloadTableEntry> {
		public final String name;
		public final String relativeFilePath;
		public final String status;
		public final boolean restartRequired;

		public ReloadTableEntry(String name, String relativeFilePath, String status, boolean restartRequired) {
			super();
			this.name = name;
			this.relativeFilePath = relativeFilePath;
			this.status = status;
			this.restartRequired = restartRequired;
		}

		@Override
		public int compareTo(ReloadTableEntry o) {
			return relativeFilePath.compareTo(o.relativeFilePath);
		}

	}
}
