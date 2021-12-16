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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AqmPropertyParser {
    private static final Logger log = LoggerFactory.getLogger(AqmPropertyParser.class);

    private HashMap<String, String> propertyHash = new HashMap<>();
    private List<AqmProperty> propertyList = new ArrayList<>();

    public AqmPropertyParser() {
    }

    ;

    public AqmPropertyParser(String properties) {
        parse(properties);
    }

    private void parse(String properties) {
        try (BufferedReader reader = new BufferedReader(new StringReader(properties))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("["))
                    continue;

                String key = line.replaceFirst("([^=]+) = (.*)", "$1");
                String value = line.replaceFirst("([^=]+) = (.*)", "$2");
                propertyHash.put(key, value);
                propertyList.add(new AqmProperty(key, value));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getStringProperty(String key) {
        return propertyHash.get(key);
    }

    public int getIntProperty(String key) {
        return Integer.parseInt(propertyHash.get(key));
    }

    public double getDoubleProperty(String key) {
        return Double.parseDouble(propertyHash.get(key));
    }

    public Date getDateProperty(String key) {
        Date d = null;
        try {
            d = new SimpleDateFormat("yyyy/mm/dd").parse(propertyHash.get(key));
        } catch (ParseException e) {
            log.debug("Can not parse date format yyyy/mm/dd : " + propertyHash.get(key));
        }
        return d;
    }

    public List<AqmProperty> getPropertyList() {
        return propertyList;
    }

    public void printAll() {
        for (AqmProperty property : propertyList)
            log.debug(property.key + "=" + property.value);
    }
}