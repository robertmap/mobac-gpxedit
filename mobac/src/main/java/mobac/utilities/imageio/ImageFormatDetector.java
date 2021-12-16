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
package mobac.utilities.imageio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ImageFormatDetector {

    private static final Logger LOG = LoggerFactory.getLogger(ImageFormatDetector.class);

    private static final int MAGIC_JPEG = 0xFFD8FF00; // 3 of 4 bytes
    private static final int MAGIC_PNG = 0x89504E47;

    public static FormatEnum detectFormat(byte[] data) {
        if (data == null || data.length < 4) {
            return FormatEnum.UNKNOWN;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int magic = in.readInt();

            if (magic == MAGIC_PNG) {
                return FormatEnum.PNG;
            }

            if ((magic & 0xFFFFFF) == MAGIC_JPEG) {
                return FormatEnum.JPEG;
            }


        } catch (IOException e) {
            LOG.error("Failed to detect image format: " + e.getMessage(), e);
            return FormatEnum.UNKNOWN;
        }
        return FormatEnum.UNKNOWN;
    }

    public enum FormatEnum {
        JPEG, PNG, UNKNOWN
    }
}
