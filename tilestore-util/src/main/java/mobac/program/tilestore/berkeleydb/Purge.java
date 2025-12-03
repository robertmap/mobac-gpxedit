package mobac.program.tilestore.berkeleydb;

import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.ts_util.Main;
import mobac.ts_util.ParamTests;

import java.io.File;
import java.security.InvalidParameterException;

public class Purge implements Runnable {

	private final File databaseDir;

	public Purge(String databaseDir) {
		this.databaseDir = new File(databaseDir);
		if (!ParamTests.testBerkelyDbDir(this.databaseDir)) {
			throw new InvalidParameterException();
		}
	}

	public void run() {
		BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
		try (TileDatabase db = tileStore.new TileDatabase("Source", databaseDir)) {
			Main.log.info("Database purge initiated");
			db.purge();
			Main.log.info("Database purge completed");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
