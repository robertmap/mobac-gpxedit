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
package mobac.utilities.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class CoordinateDms2Format extends NumberFormat {

	private static final Logger log = LoggerFactory.getLogger(CoordinateDms2Format.class);

	private final NumberFormat degFmt;
	private final NumberFormat minFmt;
	private final NumberFormat secFmt;
	private final NumberFormat secFmtParser;

	public CoordinateDms2Format(DecimalFormatSymbols dfs) {
		degFmt = new DecimalFormat("00°", dfs);
		minFmt = new DecimalFormat("00''", dfs);
		minFmt.setRoundingMode(RoundingMode.FLOOR);
		secFmt = new DecimalFormat("00.00\"", dfs);
		secFmt.setRoundingMode(RoundingMode.FLOOR);
		secFmtParser = new DecimalFormat("##.##", dfs);
	}

	@Override
	public StringBuffer format(double numberOrg, StringBuffer toAppendTo, FieldPosition pos) {
		double number = numberOrg;
		int degrees;
		int minutes;
		double seconds;
		if (number >= 0) {
			degrees = (int) Math.floor(number);
		} else {
			degrees = (int) Math.ceil(number);
		}
		number = Math.abs((number - degrees) * 60);
		minutes = (int) Math.floor(number);
		seconds = (number - minutes) * 60;
		if (numberOrg < 0 && degrees == 0) {
			toAppendTo.append("-");
		}
		toAppendTo.append(degFmt.format(degrees) + " ");
		toAppendTo.append(minFmt.format(minutes) + " ");
		toAppendTo.append(secFmt.format(seconds));
		return toAppendTo;
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Number parse(String source) {
		return parse(source, new ParsePosition(0));
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		String[] tokens = source.trim().split("[°\\'\\\"]");
		if (tokens.length != 3) {
			return null;
		}
		try {
			String degStr = tokens[0].trim();
			int deg = Integer.parseInt(degStr);
			int min = Integer.parseInt(tokens[1].trim());
			double sec = secFmtParser.parse(tokens[2].trim()).doubleValue();
			double coord;
			if (degStr.startsWith("-")) {
				coord = deg - sec / 3600 - min / 60.0;
			} else {
				coord = deg + sec / 3600 + min / 60.0;
			}
			return coord;
		} catch (Exception e) {
			parsePosition.setErrorIndex(0);
			log.error(e.getMessage(), e);
			return null;
		}
	}

}
