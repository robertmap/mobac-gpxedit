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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.mapsources.custom.aqm;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class MetaDataHeader {
	private static final Logger log = Logger.getLogger(MetaDataHeader.class);

	private static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

	public static final String FLAT_PACK_HEADER = "FLATPACK1";

	private static final String FLAT_PACK_SEPARATOR = "\0";
	private static final byte FLAT_PACK_BSEPARATOR = 0;
	private static final String AQM_END_DELIMITER = "#END";

	private String metaDataHeader;

	public MetaDataHeader(File fileAQMmap) {
		byte[] tHeader = getFileHeadder(fileAQMmap);
		metaDataHeader = new String(tHeader, ISO_8859_1);
	}

	public String getMetaDataHeader() {
		return metaDataHeader;
	}

	private byte[] getFileHeadder(File fileAQMmap) {
		byte[] bHeader = null;
		String size = "";
		try {
			try (ByteArrayOutputStream bosHeaderSize = new ByteArrayOutputStream()) {
				// read header
				try (FileInputStream fis = new FileInputStream(fileAQMmap)) {
					fis.skip(FLAT_PACK_HEADER.length());
					int b = 1;
					do {
						b = fis.read();
						if (b != FLAT_PACK_BSEPARATOR)
							bosHeaderSize.write(b);
					} while (b != FLAT_PACK_BSEPARATOR);
				}
				size = new String(bosHeaderSize.toByteArray(), ISO_8859_1);
			}
			int len = Integer.parseInt(size);

			int headerSize = len + FLAT_PACK_SEPARATOR.getBytes().length + AQM_END_DELIMITER.getBytes().length;
			bHeader = new byte[headerSize];
			try (DataInputStream in = new DataInputStream(new FileInputStream(fileAQMmap))) {
				in.readFully(bHeader);
			}

			// read content size

			byte[] bContentSize;
			try (ByteArrayOutputStream bosContentSize = new ByteArrayOutputStream()) {
				try (FileInputStream fis = new FileInputStream(fileAQMmap)) {
					int b = 1;
					fis.skip(headerSize + 1);
					do {
						b = fis.read();
						if (b != FLAT_PACK_BSEPARATOR)
							bosContentSize.write(b);
					} while (b != FLAT_PACK_BSEPARATOR);
				}

				bContentSize = bosContentSize.toByteArray();
				size = new String(bContentSize, ISO_8859_1);
			}
			len = Integer.parseInt(size);

			// append header and content size
			ByteArrayOutputStream bosHeader = new ByteArrayOutputStream();
			bosHeader.write(bHeader);
			bosHeader.write(FLAT_PACK_BSEPARATOR);
			bosHeader.write(bContentSize);
			bosHeader.write(FLAT_PACK_BSEPARATOR);
			bHeader = bosHeader.toByteArray();

		} catch (IOException e) {
			log.debug("Can not create FileInputStream for file : " + fileAQMmap.getAbsolutePath());
		} catch (NumberFormatException e) {
			log.debug("Can not parseInt for string : " + size);
		}
		return bHeader;
	}

}
