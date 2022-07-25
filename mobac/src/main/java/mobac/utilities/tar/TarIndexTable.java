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
package mobac.utilities.tar;

import java.util.Hashtable;

public class TarIndexTable {

	/**
	 * Maps tile name to TAR block index (each block has 512 bytes).
	 */
	private final Hashtable<String, Long> hashTable;

	public TarIndexTable(int initialCapacity) {
		hashTable = new Hashtable<>(initialCapacity);
	}

	public void addTarEntry(String filename, long streamPos) {
		assert ((streamPos & 0x1F) == 0);
		hashTable.put(filename, streamPos);
	}

	public long getEntryOffset(String filename) {
		Long tarBlockIndex = hashTable.get(filename);
		if (tarBlockIndex == null) {
			return -1;
		}
		return tarBlockIndex;
	}

	public int size() {
		return hashTable.size();
	}
}
