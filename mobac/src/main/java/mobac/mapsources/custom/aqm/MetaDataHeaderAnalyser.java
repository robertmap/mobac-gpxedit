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

import java.util.ArrayList;
import java.util.List;

/**
 * Alpine Quest Map : https://alpinequest.net/ Developer :
 * ph-t@users.sourceforge.net
 */
public class MetaDataHeaderAnalyser {

	public static final String AQM_VERSION = "2";
	public static final String AQM_HEADER = "V" + AQM_VERSION + "HEADER";
	public static final String AQM_LEVEL = "V" + AQM_VERSION + "LEVEL";
	public static final String AQM_LEVEL_DELIMITER = "@LEVEL";
	public static final String AQM_END_DELIMITER = "#END";
	private static final Logger log = LoggerFactory.getLogger(MetaDataHeaderAnalyser.class);
	private final List<String> tokens;
	private final List<MetaDataLevel> levelList;
	private int nbFiles;
	private long byteArrayStartIndex;
	private long byteArrayEndIndex;

	public MetaDataHeaderAnalyser(List<String> tokens) {
		this.tokens = tokens;
		this.levelList = new ArrayList<>();
		buildLevelList();
	}

	public int getNbFiles() {
		return nbFiles;
	}

	public long getByteArrayStartIndex() {
		return byteArrayStartIndex;
	}

	public long getByteArrayEndIndex() {
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
				nbFiles = Integer.parseInt(currentToken);
				j++;
			} else if (currentToken.equals(AQM_HEADER)) {
				byteArrayStartIndex = Long.parseLong(tokens.get(++j));
				j++;
			} else if (currentToken.equals(AQM_LEVEL)) {
				currentToken = tokens.get(++j);
				levelList.add(new MetaDataLevel(Long.parseLong(currentToken)));
				j++;
			} else if (currentToken.equals(AQM_LEVEL_DELIMITER)) {
				log.trace("{} start at {}", AQM_LEVEL_DELIMITER, j);
				MetaDataLevel currentLevel = levelList.get(currentLevelIndex);
				currentToken = tokens.get(++j);
				currentLevel.byteIndex = Long.parseLong(currentToken);
				currentToken = tokens.get(++j);
				while (tokens.size() > j
						&& (!currentToken.equals(AQM_LEVEL_DELIMITER) && !currentToken.equals(AQM_END_DELIMITER))) {
					String nextToken = tokens.get(j + 1);
					log.trace("Tile {} {}", currentToken, nextToken);
					MetaDataTile currentTile = new MetaDataTile(nextToken, currentToken);
					currentLevel.addTile(currentTile);
					j += 2;
					if (j >= tokens.size()) {
						return;
					}
					currentToken = tokens.get(j);
				}
				currentLevelIndex++;
			} else if (currentToken.equals(AQM_END_DELIMITER)) {
				byteArrayEndIndex = Long.parseLong(tokens.get(++j));
				j++;
			} else {
				j++;
			}
		}

	}
}
