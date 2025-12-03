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
package mobac.program.jaxb;

import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class BooleanAdapter extends XmlAdapter<String, Boolean> {
	@Override
	public Boolean unmarshal(String v) throws Exception {
		if ("true".equals(v)) {
			return true;
		}
		if ("false".equals(v)) {
			return false;
		}
		throw new UnmarshalException("Invalid boolean value: \"" + v + "\" - allowed is \"true\" or \"false\"");
	}

	@Override
	public String marshal(Boolean v) {
		return Boolean.toString(v);
	}

}
