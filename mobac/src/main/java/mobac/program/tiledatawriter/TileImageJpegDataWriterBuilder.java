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
package mobac.program.tiledatawriter;

import mobac.program.interfaces.TileImageDataWriterBuilder;
import mobac.program.model.TileImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageWriter;

public class TileImageJpegDataWriterBuilder implements TileImageDataWriterBuilder {

	protected static final Logger log = LoggerFactory.getLogger(TileImageJpegDataWriterBuilder.class);

	protected ImageWriter jpegImageWriter = null;

	protected float jpegCompressionLevel;

	/**
	 * @param jpegCompressionLevel
	 *            a float between 0 and 1; 1 specifies minimum compression and
	 *            maximum quality
	 */
	public TileImageJpegDataWriterBuilder(double jpegCompressionLevel) {
		this((float) jpegCompressionLevel);
	}

	public TileImageJpegDataWriterBuilder(float jpegCompressionLevel) {
		this.jpegCompressionLevel = jpegCompressionLevel;
	}

	public TileImageJpegDataWriterBuilder(TileImageJpegDataWriterBuilder jpegWriter) {
		this(jpegWriter.getJpegCompressionLevel());
	}

	public TileImageJpegDataWriter build() {
		return new TileImageJpegDataWriter(jpegCompressionLevel);
	}

	public float getJpegCompressionLevel() {
		return jpegCompressionLevel;
	}

	public void setJpegCompressionLevel(float jpegCompressionLevel) {
		this.jpegCompressionLevel = jpegCompressionLevel;
	}

	public TileImageType getType() {
		return TileImageType.JPG;
	}

}
