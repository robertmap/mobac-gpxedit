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
package mobac.mapsources.custom;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import mobac.exceptions.MapSourceInitializationException;
import mobac.mapsources.MapsforgeMapSource;
import mobac.program.DirectoryManager;
import mobac.program.interfaces.ReloadableMapSource;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Requires the OpenStreetMap
 */
@XmlRootElement(name = "mapsforge")
public class CustomMapsforge extends MapsforgeMapSource implements ReloadableMapSource<CustomMapsforge> {

	private static final Logger log = LoggerFactory.getLogger(CustomMapsforge.class);

	@XmlElement(nillable = false, defaultValue = "MapsforgeCustom")
	private String name = "MapsforgeCustom";

	@XmlElement(name = "mapFile")
	private String[] mapFileNames;

	@XmlElement(name = "xmlRenderTheme", defaultValue = "")
	private String xmlRenderThemeFileName = "";

	@XmlElement(defaultValue = "0")
	private int minZoom = 0;

	@XmlElement(defaultValue = "20")
	private int maxZoom = 20;

	public CustomMapsforge() {
		super();
	}

	@Override
	public void initialize() throws MapSourceInitializationException {
		xmlRenderThemeFileName = xmlRenderThemeFileName.trim();

		// The custom map xml file used for loading this map
		MapSourceLoaderInfo loaderInfo = getLoaderInfo();
		File mapSourceXmlDir = null;
		if (loaderInfo != null) {
			File mapSourceXmlFile = this.getLoaderInfo().getSourceFile();
			mapSourceXmlDir = mapSourceXmlFile.getParentFile();
		}

		List<File> newMapFileList = new ArrayList<>();
		for (String mapFileName : mapFileNames) {
			File mapFile = Utilities.findFile(mapFileName, mapSourceXmlDir, DirectoryManager.currentDir,
					DirectoryManager.mapSourcesDir, DirectoryManager.mobacUserAppDataDir);
			if (mapFile == null) {
				JOptionPane.showMessageDialog(null, "Unable to find map file \"" + mapFileName + "\"",
						I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			newMapFileList.add(mapFile);
		}
		this.mapFileList = newMapFileList;

		if (xmlRenderThemeFileName.length() > 0) {
			File renderFile = Utilities.findFile(xmlRenderThemeFileName, mapSourceXmlDir, DirectoryManager.currentDir,
					DirectoryManager.mapSourcesDir, DirectoryManager.mobacUserAppDataDir);
			if (renderFile == null) {
				JOptionPane.showMessageDialog(null,
						"Unable to find xmlRenderTheme file \"" + xmlRenderThemeFileName + "\"",
						I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
				return;
			}

			try {
				loadExternalRenderTheme(renderFile);
			} catch (FileNotFoundException e) {
				log.error("", e);
				return;
			}
		}
		super.initialize();
	}

	@Override
	public void applyChangesFrom(CustomMapsforge reloadedMapSource) throws MapSourceInitializationException {
		if (!name.equals(reloadedMapSource.getName())) {
			throw new MapSourceInitializationException("The map name has changed");
		}
		mapFileNames = reloadedMapSource.mapFileNames;
		xmlRenderThemeFileName = reloadedMapSource.xmlRenderThemeFileName;
		minZoom = reloadedMapSource.minZoom;
		maxZoom = reloadedMapSource.maxZoom;
		loaderInfo = reloadedMapSource.loaderInfo;
		initialize();
	}

	@Override
	public String getName() {
		return name;
	}

	public int getMaxZoom() {
		return maxZoom;
	}

	public int getMinZoom() {
		return minZoom;
	}

	@Override
	public String toString() {
		return name;
	}

}
