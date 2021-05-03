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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.program.tilestore.berkeleydb;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

import com.sleepycat.persist.EntityCursor;

import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.ts_util.Main;
import mobac.ts_util.ParamTests;

public class Delete implements Runnable {

	final List<String> conditions;
	final File dbDir;

	public Delete(String dbDir, List<String> conditions) {
		this.conditions = conditions;
		this.dbDir = new File(dbDir);
		if (!ParamTests.testBerkelyDbDir(this.dbDir))
			throw new InvalidParameterException();
	}

	@Override
	public void run() {

		List<DeleteTileFilter> tileFilters = new LinkedList<>();
		for (String cond : conditions) {
			String[] conditionSplit = cond.split(":", 2);
			if (conditionSplit.length == 2) {
				String filterOn = conditionSplit[0].toLowerCase().trim();
				String filterValue = conditionSplit[1].trim();
				switch (filterOn) {
				case "etag":
					tileFilters.add(new ETagDeleteTileFilter(filterValue));
					continue;
				case "zoom":
				case "z":
					tileFilters.add(new ZoomDeleteTileFilter(Integer.parseInt(filterValue)));
					continue;
				case "x":
					tileFilters.add(new XDeleteTileFilter(Integer.parseInt(filterValue)));
					continue;
				case "y":
					tileFilters.add(new YDeleteTileFilter(Integer.parseInt(filterValue)));
					continue;
				}
			}
			System.err.println("Invalid condition: " + cond);
			System.exit(-1);

		}
		System.out.println("Deleting all tiles that match all of the following condition(s):");
		for (DeleteTileFilter tf : tileFilters) {
			System.out.println("\t" + tf.getInfoMessage());
		}

		BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
		TileDatabase db = null;
		try {
			db = tileStore.new TileDatabase("Db", dbDir);
			Main.log.info("Tile store entry count: " + db.entryCount() + " (before deleting)");
			EntityCursor<TileDbEntry> cursor = db.getTileIndex().entities();
			try {
				TileDbEntry entry;
				cursorLoop: while ((entry = cursor.next()) != null) {
					for (DeleteTileFilter tf : tileFilters) {
						if (!tf.canDeleteTile(entry)) {
							continue cursorLoop;
						}
					}
					Main.log.trace("Deleting " + entry);
					cursor.delete();
				}
			} finally {
				cursor.close();
			}
			Main.log.info("Tile store entry count: " + db.entryCount() + " (after deleting)");
		} catch (Exception e) {
			Main.log.error("Deleting of tiles failed", e);
		} finally {
			db.close(false);
		}
	}

	public interface DeleteTileFilter {
		public boolean canDeleteTile(TileDbEntry entry);

		public String getInfoMessage();
	}

	public static class ETagDeleteTileFilter implements DeleteTileFilter {

		final String eTagValue;

		public ETagDeleteTileFilter(String eTagValue) {
			super();
			this.eTagValue = eTagValue;
		}

		@Override
		public boolean canDeleteTile(TileDbEntry entry) {
			String eTag = "" + entry.geteTag(); // Allows to filter for null value
			if (eTag.startsWith("\"") && eTag.endsWith("\"")) {
				eTag = eTag.substring(1, eTag.length() - 1);
			}
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
