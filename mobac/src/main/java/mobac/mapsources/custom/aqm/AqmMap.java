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
package mobac.mapsources.custom.aqm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AqmMap {
	private static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
	private static final Logger log = LoggerFactory.getLogger(AqmMap.class);
	private static final String FLAT_PACK_SEPARATOR = "\0";
	private static final byte FLAT_PACK_BSEPARATOR = FLAT_PACK_SEPARATOR.getBytes()[0];
	private final File fileAQMmap;
	private final MetaDataHeader header;
	private final long headerSize;
	private final MetaDataHeaderAnalyser headerAnalyser;
	private final List<AqmLevel> levels = new ArrayList<>();
	private final Map<String, AqmTile> tilesMap = new HashMap<>();
	// map properties
	public String id;
	public String name;
	public int version;
	public Date date;
	public String creator;
	public String software;
	public int minZoom = -1;
	public int maxZoom = -1;
	public String imgFormat;

	public AqmMap(File fileAQMmap) throws IOException {
		this.fileAQMmap = fileAQMmap;
		this.header = new MetaDataHeader(fileAQMmap);
		this.headerSize = header.getHeaderSize();
		this.headerAnalyser = new MetaDataHeaderAnalyser(header.getTokenizedHeader());
		buildMap();
	}

	private static String generateKey(int zoom, int y, int x) {
		return String.format("%d_%d_%d", zoom, y, x);
	}

	private String getFileChunkString(long start) throws IOException {
		return new String(getFileChunk(start), ISO_8859_1);
	}

	private byte[] getFileChunk(long start) throws IOException {
		try (InputStream in = new BufferedInputStream(new FileInputStream(fileAQMmap))) {
			if (in.skip(start) != start) {
				throw new IOException("Skipping failed");
			}
			int chunkSize = MetaDataHeader.readZeroTerminatedIntegerString(in);
			return in.readNBytes(chunkSize);
		}
	}

	private void buildMap() throws IOException {
		String mapMetaData = getFileChunkString(headerSize);
		AqmPropertyParser mapProperties = new AqmPropertyParser(mapMetaData);
		this.id = mapProperties.getStringProperty("id");
		this.name = mapProperties.getStringProperty("name");
		this.version = mapProperties.getIntProperty("version");
		this.date = mapProperties.getDateProperty("date");
		this.creator = mapProperties.getStringProperty("creator");
		this.software = mapProperties.getStringProperty("software");

		List<MetaDataLevel> levelList = headerAnalyser.getLevelList();
		for (MetaDataLevel l : levelList) {

			String levelMetaData = getFileChunkString(headerSize + l.metaDataByteIndex);
			AqmPropertyParser levelProperties = new AqmPropertyParser(levelMetaData);
			AqmLevel level = new AqmLevel(levelProperties);
			this.minZoom = ((this.minZoom == -1) ? level.z : Math.min(level.z, this.minZoom));
			this.maxZoom = ((this.maxZoom == -1) ? level.z : Math.max(level.z, this.maxZoom));
			this.imgFormat = level.imgformat;

			this.levels.add(level);
			List<MetaDataTile> tileList = l.getTileList();
			for (MetaDataTile t : tileList) {
				// tile loaded in memory
				// byte[] bTile = getFileChunk(headerSize + t.byteIndex);
				// AqmTile tile = new AqmTile(t.name, bTile, headerSize + t.byteIndex);

				AqmTile tile = new AqmTile(t.name, headerSize + t.byteIndex);
				level.tiles.add(tile);
				tilesMap.put(generateKey(level.z, tile.y, tile.x), tile);
			}
		}
	}

	private AqmTile getTile(int zoom, int x, int y) {
		return tilesMap.get(generateKey(zoom, y, x));
	}

	public byte[] getByteTile(int zoom, int x, int y) throws IOException {
		AqmTile t = getTile(zoom, x, y);
		if (t == null) {
			log.debug("getByteTile : zoom : {} x : {} y : {} :: Not Found", zoom, x, y);
			return null;
		}
		byte[] bTile;
		log.debug("getByteTile : zoom : {} x : {} y : {} :: Found", zoom, x, y);
		if (t.bTile != null) {
			bTile = t.bTile;
		} else {
			AqmTile tile = tilesMap.get(generateKey(zoom, y, x));
			bTile = getFileChunk(tile.tileByteIndex);
		}
		return bTile;
	}

	public List<AqmLevel> getLevelList() {
		return levels;
	}

	public int getMinZoomXtCenter() {
		AqmLevel level = levels.get(0);
		return level.getXtCenter() * level.xtsize;
	}

	public int getMinZoomYtCenter() {
		AqmLevel level = levels.get(0);
		int mapSize = level.ytsize << level.z;
		return mapSize - level.getYtCenter() * level.ytsize;
	}
}
