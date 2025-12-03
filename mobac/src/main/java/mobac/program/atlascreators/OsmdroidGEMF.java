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
package mobac.program.atlascreators;

import mobac.program.annotations.AtlasCreatorName;
import mobac.program.atlascreators.impl.gemf.GEMFFileCreator;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * AtlasCreator implementation to create a GEMF archive file. For details about
 * the format, please see the link in {@link GEMFFileCreator}.
 *
 * @author M. Reiter
 *
 */
@AtlasCreatorName("Osmdroid GEMF")
public class OsmdroidGEMF extends OSMTracker {

	private static final String GEMF_FILE_EXTENSION = ".gemf";

	@Override
	public void finishAtlasCreation() throws IOException, InterruptedException {
		List<File> tileFolders = new LinkedList<>();
		tileFolders.add(mapDir);

		String gemfLocation = new File(atlasDir, atlas.getName() + GEMF_FILE_EXTENSION).toString();

		new GEMFFileCreator(gemfLocation, tileFolders, log);

		super.finishAtlasCreation();
	}

}
