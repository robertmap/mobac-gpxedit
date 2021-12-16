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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

;

public class MetaDataHeaderTokenizer {

    private static final String FLAT_PACK_SEPARATOR = "\0";

    private List<String> tokens;

    public MetaDataHeaderTokenizer(String metaDataHeader) {
        tokens = tokenizeHeader(metaDataHeader);
    }

    public List<String> getTokens() {
        return tokens;
    }

    private List<String> tokenizeHeader(String metaDataHeader) {
        Pattern pattern = Pattern.compile(FLAT_PACK_SEPARATOR, Pattern.LITERAL);
        String[] parts = pattern.split(metaDataHeader, -1);

        List<String> ret = new ArrayList<>();
        for (String part : parts)
            ret.add(part);
        return ret;
    }
}
