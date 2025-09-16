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
package mobac.program.tilestore.berkeleydb;

import com.sleepycat.persist.PrimaryIndex;
import mobac.gui.mapview.PreviewMap;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.program.tilestore.berkeleydb.TileDbEntry.TileDbKey;
import mobac.ts_util.Main;
import mobac.ts_util.ParamTests;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteTiles implements Runnable {

	private final List<String> tiles;
	private final File dbDir;

	public DeleteTiles(String dbDir, List<String> tiles) {
		this.tiles = tiles;
		this.dbDir = new File(dbDir);
		if (!ParamTests.testBerkelyDbDir(this.dbDir)) {
			throw new InvalidParameterException();
		}
	}

	@Override
	public void run() {

		Pattern p = Pattern.compile("z?([0-9]+)/([0-9]+)/([0-9]+)");
		List<TileDbKey> tileKeys = new LinkedList<>();
		for (String t : tiles) {
			Matcher m = p.matcher(t);
			boolean valid = m.matches();

			int zoom = -1;
			int x = -1;
			int y = -1;
			if (valid) {
				zoom = Integer.parseInt(m.group(1));
				x = Integer.parseInt(m.group(2));
				y = Integer.parseInt(m.group(3));
				valid &= (zoom >= PreviewMap.MIN_ZOOM) && (zoom <= PreviewMap.MAX_ZOOM);
				valid &= (x >= 0) && (y >= 0);
			}

			if (!valid) {
				System.err.println("Invalid tile coordinate: " + t);
				System.exit(-1);
			}

			tileKeys.add(new TileDbKey(x, y, zoom));
		}
		System.out.println("Deleting the following tiles:");
		for (TileDbKey key : tileKeys) {
			System.out.println("\t" + key);
		}

		BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
		try (TileDatabase db = tileStore.new TileDatabase("Db", dbDir)) {
			Main.log.info("Tile store entry count: {} (before deleting)", db.entryCount());
			PrimaryIndex<TileDbKey, TileDbEntry> tileIndex = db.getTileIndex();
			for (TileDbKey key : tileKeys) {
				if (!tileIndex.delete(key)) {
					Main.log.trace("Failed to delete {}", key);
				}
			}
			db.purge();
			Main.log.info("Tile store entry count: {} (after deleting)", db.entryCount());
		} catch (Exception e) {
			Main.log.error("Deleting of tiles failed", e);
		}
	}

	public interface DeleteTileFilter {
		boolean canDeleteTile(TileDbEntry entry);

		String getInfoMessage();
	}

	public static class ETagDeleteTileFilter implements DeleteTileFilter {

		final String eTagValue;

		public ETagDeleteTileFilter(String eTagValue) {
			super();
			this.eTagValue = eTagValue;
		}

		@Override
		public boolean canDeleteTile(TileDbEntry entry) {
			String eTag =  entry.geteTag(); // Allows to filter for null value
			return eTag.equals(eTagValue);
		}

		@Override
		public String getInfoMessage() {
			return "tiles with an etag of: \"" + eTagValue + "\"";
		}

	}

	public static class ZoomDeleteTileFilter implements DeleteTileFilter {

		final int zoom;

		public ZoomDeleteTileFilter(int zoom) {
			super();
			this.zoom = zoom;
		}

		@Override
		public boolean canDeleteTile(TileDbEntry entry) {
			return entry.getZoom() == zoom;
		}

		@Override
		public String getInfoMessage() {
			return "tiles with an zoom level of " + zoom;
		}

	}

	public static class XDeleteTileFilter implements DeleteTileFilter {

		final int x;

		public XDeleteTileFilter(int x) {
			super();
			this.x = x;
		}

		@Override
		public boolean canDeleteTile(TileDbEntry entry) {
			return entry.getX() == x;
		}

		@Override
		public String getInfoMessage() {
			return "tiles with an x coordinate of " + x;
		}

	}

	public static class YDeleteTileFilter implements DeleteTileFilter {

		final int y;

		public YDeleteTileFilter(int y) {
			super();
			this.y = y;
		}

		@Override
		public boolean canDeleteTile(TileDbEntry entry) {
			return entry.getY() == y;
		}

		@Override
		public String getInfoMessage() {
			return "tiles with an y coordinate of " + y;
		}
	}
}
