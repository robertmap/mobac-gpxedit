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

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import mobac.mapsources.custom.CustomWmsMapSource.CoordinateUnit;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CoordinateUnitAdapter extends XmlAdapter<String, CoordinateUnit> {

	@Override
	public CoordinateUnit unmarshal(String v) {
		try {
			return CoordinateUnit.valueOf(v.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			String values = Arrays.stream(CoordinateUnit.values()).map(x -> "\"" + x.name().toLowerCase() + "\"")
					.collect(Collectors.joining(", "));
			throw new RuntimeException(String.format("Invalid coordinateunit \"%s\" possible values: %s", v, values));
		}
	}

	@Override
	public String marshal(CoordinateUnit v) {
		return v.name();
	}

}
