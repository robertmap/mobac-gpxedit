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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Alpine Quest Map : https://alpinequest.net/ Developer :
 * ph-t@users.sourceforge.net
 */
public class AqmPropertyParser {

	private final HashMap<String, String> propertyMap = new HashMap<>();

	public AqmPropertyParser(String properties) throws IOException {
		parse(properties);
	}

	private void parse(String properties) throws IOException {
		try (BufferedReader reader = new BufferedReader(new StringReader(properties))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0 || line.startsWith("[")) {
					continue;
				}

				String key = line.replaceFirst("([^=]+) = (.*)", "$1");
				String value = line.replaceFirst("([^=]+) = (.*)", "$2");
				propertyMap.put(key, value);
			}
		}
	}

	public String getStringProperty(String key) {
		return propertyMap.get(key);
	}

	public int getIntProperty(String key) {
		return Integer.parseInt(propertyMap.get(key));
	}

	public double getDoubleProperty(String key) {
		return Double.parseDouble(propertyMap.get(key));
	}

	public Date getDateProperty(String key) {
		try {
			return new SimpleDateFormat("yyyy/MM/dd").parse(propertyMap.get(key));
		} catch (ParseException e) {
			throw new RuntimeException("Can not parse date format yyyy/MM/dd : " + propertyMap.get(key));
		}
	}

}
