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
package mobac.program.model;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;

public class MapSourceLoaderInfo {

	protected final LoaderType loaderType;

	protected final File sourceFile;
	protected final long sourceFileLastModified;
	protected final long sourceFileSize;
	protected final String revision;

	public MapSourceLoaderInfo(LoaderType loaderType, File sourceFile) {
		this(loaderType, sourceFile, null);
	}

	public MapSourceLoaderInfo(LoaderType loaderType, File sourceFile, String revision) {
		super();
		this.loaderType = loaderType;
		this.sourceFile = sourceFile;
		if (sourceFile != null) {
			this.sourceFileLastModified = sourceFile.lastModified();
			this.sourceFileSize = sourceFile.length();
		} else {
			this.sourceFileLastModified = -1;
			this.sourceFileSize = -1;
		}
		this.revision = revision;
	}

	public LoaderType getLoaderType() {
		return loaderType;
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public Path getSourceFileRelativePath() {
		Path mapSourceDir = Settings.getInstance().getMapSourcesDirectory().toPath();
		return mapSourceDir.relativize(sourceFile.toPath());
	}

	public long getSourceFileLastModified() {
		return sourceFileLastModified;
	}

	public long getSourceFileSize() {
		return sourceFileSize;
	}

	public String getRevision() {
		return revision;
	}

	/**
	 * Check if the modification date or the file size of the source file has
	 * changed
	 *
	 * @return <code>true</code> if a change was detected
	 */
	public boolean checkSourcesfileChanged() {
		return (sourceFile.lastModified() != sourceFileLastModified) || (sourceFile.length() != sourceFileSize);
	}

	/**
	 * This method gets a MapSource's path, relative to the /mapsources folder.
	 *
	 * @return Path - relative path to the /mapsources folder, returns null if the
	 *         file is directly in the /mapsources folder
	 */
	public String[] getRelativePath() {
		File mapSourcesDir = Settings.getInstance().getMapSourcesDirectory();
		File dir = sourceFile.getParentFile();
		LinkedList<String> pathList = new LinkedList<>();
		while (!mapSourcesDir.equals(dir)) {
			pathList.offerFirst(dir.getName());
			dir = dir.getParentFile();
		}
		if (pathList.isEmpty()) {
			return null;
		}
		return pathList.toArray(new String[pathList.size()]);
	}

	public enum LoaderType {
		MAPPACK("Mappack"), // map pack file
		XML("Custom XML"), // custom map xml
		BSH("BeanShell") // BeanShell script
		;

		public final String displayName;

		LoaderType(String displayName) {
			this.displayName = displayName;
		}
	}
}
