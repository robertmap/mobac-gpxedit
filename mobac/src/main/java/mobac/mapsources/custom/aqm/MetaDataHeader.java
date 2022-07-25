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

import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MetaDataHeader {
	public static final byte[] FLAT_PACK_HEADER = "FLATPACK1".getBytes();
	private static final Logger log = LoggerFactory.getLogger(MetaDataHeader.class);
	private static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
	private static final String FLAT_PACK_SEPARATOR = "\0";
	private static final byte FLAT_PACK_BSEPARATOR = 0;

	private final byte[] headerBytes;
	private final int headerSize;

	public MetaDataHeader(File fileAQMmap) throws IOException {
		try (CountingInputStream in = new CountingInputStream(
				new BufferedInputStream(new FileInputStream(fileAQMmap)))) {
			byte[] header = in.readNBytes(FLAT_PACK_HEADER.length);
			if (!Arrays.equals(header, FLAT_PACK_HEADER)) {
				throw new IOException("File does not start with " + new String(FLAT_PACK_HEADER));
			}
			int len = readZeroTerminatedIntegerString(in);
			headerBytes = in.readNBytes(len);
			headerSize = in.getCount();
		}
	}

	public static int readZeroTerminatedIntegerString(InputStream in) throws IOException {
		String s = readZeroTerminatedString(in);
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			String msg = "Can not parseInt for string: \"" + s + "\"";
			if (in instanceof CountingInputStream) {
				CountingInputStream cin = (CountingInputStream) in;
				msg += " string end at " + cin.getByteCount();
			}
			throw new IOException(msg);
		}
	}

	public static String readZeroTerminatedString(InputStream in) throws IOException {
		return new String(readZeroTerminatedStringBytes(in), ISO_8859_1);
	}

	public static byte[] readZeroTerminatedStringBytes(InputStream in) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		while (true) {
			int b = in.read();
			if (b == -1) {
				throw new EOFException();
			}
			if (b == FLAT_PACK_BSEPARATOR) {
				return bout.toByteArray();
			}
			bout.write(b);
		}
	}

	public List<String> getTokenizedHeader() {
		String metaDataHeader = new String(headerBytes, ISO_8859_1);
		Pattern pattern = Pattern.compile(FLAT_PACK_SEPARATOR, Pattern.LITERAL);
		String[] parts = pattern.split(metaDataHeader, -1);
		return List.of(parts);
	}

	public byte[] getHeaderBytes() {
		return headerBytes;
	}

	public int getHeaderSize() {
		return headerSize;
	}
}
