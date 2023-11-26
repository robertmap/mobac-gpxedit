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
package mobac.mapsources.loader;

import mobac.mapsources.MapSourcesManager;
import mobac.mapsources.custom.BeanShellHttpMapSource;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.MapSourceLoaderInfo.LoaderType;
import mobac.utilities.Utilities;
import mobac.utilities.file.DirOrFileExtFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.io.File;
import java.util.List;

public class BeanShellMapSourceLoader {

	private final Logger log = LoggerFactory.getLogger(BeanShellMapSourceLoader.class);
	private final MapSourcesManager mapSourcesManager;
	private final File mapSourcesDir;

	public BeanShellMapSourceLoader(MapSourcesManager mapSourceManager, File mapSourcesDir) {
		this.mapSourcesManager = mapSourceManager;
		this.mapSourcesDir = mapSourcesDir;
	}

	public void loadBeanShellMapSources() {
		List<File> customMapSourceFiles = getMapSourceFiles();
		for (File f : customMapSourceFiles) {
			try {
				BeanShellHttpMapSource mapSource = BeanShellHttpMapSource.load(f);
				MapSourceLoaderUtils.testMapSourceName(mapSource.getName());
				log.trace("BeanShell map source loaded: " + mapSource + " from file \"" + f.getName() + "\"");
				mapSource.setLoaderInfo(new MapSourceLoaderInfo(LoaderType.BSH, f));
				mapSourcesManager.addMapSource(mapSource);
			} catch (Exception e) {
				String errorMsg = "Failed to load custom BeanShell map source \"" + f.getName() + "\": "
						+ e.getMessage();
				log.error(errorMsg, e);
				JOptionPane.showMessageDialog(null, errorMsg, "Failed to load custom BeanShell map source",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public List<File> getMapSourceFiles() {
		return Utilities.traverseFolder(mapSourcesDir, new DirOrFileExtFilter(".bsh"));
	}

}
