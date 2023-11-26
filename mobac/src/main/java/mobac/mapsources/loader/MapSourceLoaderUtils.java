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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MapSourceLoaderUtils {
	public static void testMapSourceName(String name) throws RuntimeException {
		Path p;
		try {
			p = Paths.get(name); // validate name
		} catch (InvalidPathException e) {
			throw new RuntimeException("Invalid custom map name: " + name);
		}
		if (p.getNameCount() > 1) {
			// name contains something like "../"
			throw new RuntimeException("Invalid custom map name (possible path traversal attack): " + name);
		}

		String[] disallowed = new String[]{"/", "\\"};
		for (String s : disallowed) {
			if (name.contains(s)) {
				throw new RuntimeException("Custom map name contains invalid character \"" + s + "\" - " + name);
			}
		}
	}
}
