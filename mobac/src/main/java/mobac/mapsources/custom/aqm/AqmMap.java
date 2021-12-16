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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AqmMap {
    protected static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
    private static final Logger log = LoggerFactory.getLogger(AqmMap.class);
    private static final String FLAT_PACK_SEPARATOR = "\0";
    private static final byte FLAT_PACK_BSEPARATOR = FLAT_PACK_SEPARATOR.getBytes()[0];
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
    private File fileAQMmap;
    // private RandomAccessFile randomAccessFile;
    private MetaDataHeader header;
    private long headerSize;
    private MetaDataHeaderAnalyser headerAnalyser;
    private MetaDataHeaderTokenizer headerTokenizer;
    private List<AqmLevel> levels = new ArrayList<AqmLevel>();
    private Map<String, AqmTile> tilesMap = new HashMap<>();

    public AqmMap(File fileAQMmap) {
        this.fileAQMmap = fileAQMmap;
        this.header = new MetaDataHeader(fileAQMmap);
        this.headerSize = header.getMetaDataHeader().length();
        this.headerTokenizer = new MetaDataHeaderTokenizer(this.header.getMetaDataHeader());
        this.headerAnalyser = new MetaDataHeaderAnalyser(this.headerTokenizer.getTokens());
        buildMap();
    }

    private static String generateKey(int zoom, int y, int x) {
        return String.format("%d_%d_%d", zoom, y, x);
    }

    private byte[] getFileChunk(long start) {
        byte[] bSplit = null;
        String size = "";
        try (FileInputStream fis = new FileInputStream(fileAQMmap)) {
            fis.skip(start);
            int b = 1;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            do {
                b = fis.read();
                if (b != FLAT_PACK_BSEPARATOR)
                    bos.write(b);
            } while (b != FLAT_PACK_BSEPARATOR);

            size = new String(bos.toByteArray(), ISO_8859_1);
            int len = Integer.parseInt(size);
            bSplit = new byte[len];
            fis.read(bSplit);
        } catch (IOException e) {
            log.debug("Can not create FileInputStream for file : " + fileAQMmap.getAbsolutePath());
        } catch (NumberFormatException e) {
            log.debug("Can not parseInt for string : " + size);
        }
        return bSplit;
    }

    private void buildMap() {
        byte[] bMapMetaData = getFileChunk(headerSize);
        String sMapMetaData = new String(bMapMetaData, ISO_8859_1);
        AqmPropertyParser mapProperties = new AqmPropertyParser(sMapMetaData);
        this.id = mapProperties.getStringProperty("id");
        this.name = mapProperties.getStringProperty("name");
        this.version = mapProperties.getIntProperty("version");
        this.date = mapProperties.getDateProperty("date");
        this.creator = mapProperties.getStringProperty("creator");
        this.software = mapProperties.getStringProperty("software");

        List<MetaDataLevel> levelList = headerAnalyser.getLevelList();
        for (MetaDataLevel l : levelList) {

            byte[] bLevelMetaData = getFileChunk(headerSize + l.metaDataByteIndex);
            String sLevelMetaData = new String(bLevelMetaData, ISO_8859_1);
            AqmPropertyParser levelProperties = new AqmPropertyParser(sLevelMetaData);
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

    public byte[] getByteTile(int zoom, int x, int y) {
        byte[] bTile = null;
        AqmTile t = getTile(zoom, x, y);
        if (t != null) {
            log.debug("getByteTile : zoom : " + zoom + " x : " + x + " y : " + y + " :: Found");
            if (t.bTile != null) {
                bTile = t.bTile;
            } else {
                // TODO: ERROR: tilesMap.get will always return null ! -> NullPointerException
                bTile = getFileChunk(tilesMap.get(generateKey(zoom, y, x)).tileByteIndex);
            }
        } else {
            log.debug("getByteTile : zoom : " + zoom + " x : " + x + " y : " + y + " :: Not Found");
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
        int invertedMinZoomYtCenter = mapSize - level.getYtCenter() * level.ytsize;
        return invertedMinZoomYtCenter;
    }
};