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

/**
 * Alpine Quest Map : http://alpinequest.net/
 * Developer : ph-t@users.sourceforge.net
 */

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MetaDataHeaderAnalyser {
    public static final String FLAT_PACK_HEADER = "FLATPACK1";

    public static final String AQM_VERSION = "2";

    public static final String AQM_HEADER = "V" + AQM_VERSION + "HEADER";
    public static final String AQM_LEVEL = "V" + AQM_VERSION + "LEVEL";
    public static final String AQM_LEVEL_DELIMITER = "@LEVEL";
    public static final String AQM_END_DELIMITER = "#END";

    protected static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    private List<String> tokens;

    private int headerSize;
    private int nbFiles;
    private int byteArrayStartIndex;
    private int byteArrayEndIndex;
    private List<MetaDataLevel> levelList;

    public MetaDataHeaderAnalyser(List<String> tokens) {
        this.tokens = tokens;
        this.levelList = new ArrayList<MetaDataLevel>();
        buildLevelList();
    }

    public int getHeaderSize() {
        return headerSize;
    }

    public int getNbFiles() {
        return nbFiles;
    }

    public int getByteArrayStartIndex() {
        return byteArrayStartIndex;
    }

    public int getByteArrayEndIndex() {
        return byteArrayEndIndex;
    }

    public List<MetaDataLevel> getLevelList() {
        return levelList;
    }

    private void buildLevelList() {
        int j = 0;
        String currentToken;
        int currentLevelIndex = 0;
        while (tokens.size() > j) {
            currentToken = tokens.get(j);

            if (j == 0) {
                headerSize = Integer.parseInt(currentToken.substring(FLAT_PACK_HEADER.length(), currentToken.length()));
                j++;
            } else if (j == 1) {
                nbFiles = Integer.parseInt(currentToken);
                j++;
            } else if (currentToken.equals(AQM_HEADER)) {
                byteArrayStartIndex = Integer.parseInt(tokens.get(++j));
                j++;
            } else if (currentToken.equals(AQM_LEVEL)) {
                currentToken = tokens.get(++j);
                levelList.add(new MetaDataLevel(Integer.parseInt(currentToken)));
                j++;
            } else if (currentToken.equals(AQM_LEVEL_DELIMITER)) {
                MetaDataLevel currentLevel = levelList.get(currentLevelIndex);
                currentToken = tokens.get(++j);
                currentLevel.byteIndex = Integer.parseInt(currentToken);
                currentToken = tokens.get(++j);
                while (tokens.size() > j
                        && (!currentToken.equals(AQM_LEVEL_DELIMITER) && !currentToken.equals(AQM_END_DELIMITER))) {
                    String nextToken = tokens.get(j + 1);
                    MetaDataTile currentTile = new MetaDataTile(nextToken, currentToken);
                    currentLevel.tileList.add(currentTile);
                    j = j + 2;
                    currentToken = tokens.get(j);
                }
                currentLevelIndex++;
            } else if (currentToken.equals(AQM_END_DELIMITER)) {
                byteArrayEndIndex = Integer.parseInt(tokens.get(++j));
                j++;
            } else {
                j++;
            }
        }

    }
}
