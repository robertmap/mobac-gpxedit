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

import mobac.program.model.TileImageType;
import mobac.utilities.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageFormatDetector {

	private static final Logger LOG = LoggerFactory.getLogger(ImageFormatDetector.class);

	private static final byte[] PNG = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
	private static final byte[] JPG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] GIF_1 = "GIF87a".getBytes();
	private static final byte[] GIF_2 = "GIF89a".getBytes();

	private static final byte[] WEBP_RIFF = "RIFF".getBytes();
	private static final byte[] WEBP_INNER = "WEBP".getBytes();

	public static TileImageType getImageType(byte[] imageData) {
		if (imageData == null) {
			return null;
		}
		if (Utilities.startsWith(imageData, PNG)) {
			return TileImageType.PNG;
		}
		if (Utilities.startsWith(imageData, JPG)) {
			return TileImageType.JPG;
		}

		if (Utilities.startsWith(imageData, GIF_1) || Utilities.startsWith(imageData, GIF_2)) {
			return TileImageType.GIF;
		}

		if (Utilities.startsWith(imageData, WEBP_RIFF) && Utilities.startsWith(imageData, 8, WEBP_INNER)) {
			return TileImageType.WEBP;
		}

		return null;
	}
}
