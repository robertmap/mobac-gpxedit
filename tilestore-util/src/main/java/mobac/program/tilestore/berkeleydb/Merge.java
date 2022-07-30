package mobac.program.tilestore.berkeleydb;

import com.sleepycat.persist.EntityCursor;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.ts_util.Main;
import mobac.ts_util.ParamTests;

import java.io.File;
import java.security.InvalidParameterException;

public class Merge implements Runnable {

	final File sourceDir;
	final File destDir;

	public Merge(String sourceDir, String destDir) {
		this.sourceDir = new File(sourceDir);
		this.destDir = new File(destDir);
		if (!ParamTests.testBerkelyDbDir(this.sourceDir)) {
			throw new InvalidParameterException();
		}
		if (!ParamTests.testBerkelyDbDir(this.destDir)) {
			throw new InvalidParameterException();
		}
	}

	public void run() {
		BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
		try (TileDatabase dbSource = tileStore.new TileDatabase("Source", sourceDir)) {
			try (TileDatabase dbDest = tileStore.new TileDatabase("Destination", destDir)) {
				Main.log.info("Source tile store entry count: {}", dbSource.entryCount());
				Main.log.info("Destination tile store entry count: {} (before merging)", dbSource.entryCount());
				dbDest.purge();
				EntityCursor<TileDbEntry> cursor = dbSource.getTileIndex().entities();
				try {
					TileDbEntry entry = cursor.next();
					while (entry != null) {
						Main.log.trace("Adding {}", entry);
						dbDest.put(entry);
						entry = cursor.next();
					}
				} finally {
					cursor.close();
				}
				Main.log.info("Destination tile store entry count: {} (after merging)", dbSource.entryCount());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
