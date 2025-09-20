package mobac.program.tilestore.berkeleydb;

import com.sleepycat.persist.EntityCursor;
import mobac.mapsources.MapSourceTools;
import mobac.program.model.TileImageType;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.ts_util.Main;
import mobac.ts_util.ParamTests;
import mobac.utilities.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.security.InvalidParameterException;

public class Extract implements Runnable {
	private final File sourceDir;
	private final File destDir;

	public Extract(String srcDir, String destDir) {
		this.sourceDir = new File(srcDir);
		this.destDir = new File(destDir);
		if (!ParamTests.testBerkelyDbDir(this.sourceDir)) {
			throw new InvalidParameterException();
		}
		if (!ParamTests.testDir(this.destDir)) {
			throw new InvalidParameterException();
		}
	}

	public void run() {
		BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
		try (TileDatabase db = tileStore.new TileDatabase("Source", sourceDir)) {
			Main.log.info("Source tile store entry count: {}", db.entryCount());
			long count = 0;
			try (EntityCursor<TileDbEntry> cursor = db.getTileIndex().entities()) {
				TileDbEntry entry = cursor.next();
				while (entry != null) {
					Main.log.trace("Extracting {}", entry.shortInfo());
					String pattern = "{$z}/{$x}/{$y}.{$ext}";
					String fileName = MapSourceTools.formatMapUrl(pattern, entry.getZoom(), entry.getX(), entry.getY());
					byte[] data = entry.getData();
					TileImageType type = Utilities.getImageType(data);
					fileName = fileName.replace("{$ext}", type.getFileExt());

					File f = new File(destDir, fileName);
					Utilities.mkDirs(f.getParentFile());
					try (FileOutputStream fout = new FileOutputStream(f)) {
						fout.write(data);
						fout.flush();
					}
					count++;
					entry = cursor.next();
				}
			}
			Main.log.info("Number of extracted tiles: {}", count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
