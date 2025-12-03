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
package mobac.program.model;

import mobac.gui.MainGUI;
import mobac.program.interfaces.TileImageDataWriterBuilder;
import mobac.program.tiledatawriter.TileImageJpegDataWriterBuilder;
import mobac.program.tiledatawriter.TileImagePng4DataWriterBuilder;
import mobac.program.tiledatawriter.TileImagePng8DataWriterBuilder;
import mobac.program.tiledatawriter.TileImagePngDataWriterBuilder;
import mobac.utilities.I18nUtils;

import javax.swing.JComboBox;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Defines all available image formats selectable in the {@link JComboBox} in
 * the {@link MainGUI}. Each element of this enumeration contains one instance
 * of an {@link TileImageDataWriterBuilder} instance that can perform one or
 * more image operations (e.g. color reduction) and then saves the image to an
 * {@link OutputStream}.
 *
 * @see TileImageDataWriterBuilder
 * @see TileImagePngDataWriterBuilder
 * @see TileImagePng4DataWriterBuilder
 * @see TileImagePng8DataWriterBuilder
 * @see TileImageJpegDataWriterBuilder
 */
public enum TileImageFormat {

	PNG(new TileImagePngDataWriterBuilder(), "lp_tile_param_image_fmt_png"), //
	PNG8Bit(new TileImagePng8DataWriterBuilder(), "lp_tile_param_image_fmt_png_8bit"), //
	PNG4Bit(new TileImagePng4DataWriterBuilder(), "lp_tile_param_image_fmt_png_4bit"), //
	JPEG100(new TileImageJpegDataWriterBuilder(1.00), "lp_tile_param_image_fmt_jpg", 100), //
	JPEG99(new TileImageJpegDataWriterBuilder(0.99), "lp_tile_param_image_fmt_jpg", 99), //
	JPEG95(new TileImageJpegDataWriterBuilder(0.95), "lp_tile_param_image_fmt_jpg", 95), //
	JPEG90(new TileImageJpegDataWriterBuilder(0.90), "lp_tile_param_image_fmt_jpg", 90), //
	JPEG85(new TileImageJpegDataWriterBuilder(0.85), "lp_tile_param_image_fmt_jpg", 85), //
	JPEG80(new TileImageJpegDataWriterBuilder(0.80), "lp_tile_param_image_fmt_jpg", 80), //
	JPEG75(new TileImageJpegDataWriterBuilder(0.75), "lp_tile_param_image_fmt_jpg", 75), //
	JPEG70(new TileImageJpegDataWriterBuilder(0.70), "lp_tile_param_image_fmt_jpg", 70), //
	JPEG60(new TileImageJpegDataWriterBuilder(0.60), "lp_tile_param_image_fmt_jpg", 60), //
	JPEG50(new TileImageJpegDataWriterBuilder(0.50), "lp_tile_param_image_fmt_jpg", 50), //
	JPEG40(new TileImageJpegDataWriterBuilder(0.40), "lp_tile_param_image_fmt_jpg", 40), //
	JPEG30(new TileImageJpegDataWriterBuilder(0.30), "lp_tile_param_image_fmt_jpg", 30); //

	// private final String description;

	private final TileImageDataWriterBuilder dataWriterBuilder;

	private final String displayText;

	TileImageFormat(TileImageDataWriterBuilder dataWriterBuilder, String translationKey) {
		// this.description = description;
		this.dataWriterBuilder = dataWriterBuilder;
		this.displayText = I18nUtils.localizedStringForKey(translationKey);
	}

	TileImageFormat(TileImageDataWriterBuilder dataWriterBuilder, String translationKey, int value) {
		// this.description = description;
		this.dataWriterBuilder = dataWriterBuilder;
		this.displayText = I18nUtils.localizedStringForKey(translationKey, value);
	}

	public static TileImageFormat[] getPngFormats() {
		return getFormats(TileImageType.PNG);
	}

	public static TileImageFormat[] getJpgFormats() {
		return getFormats(TileImageType.JPG);
	}

	private static TileImageFormat[] getFormats(TileImageType tileImageType) {
		ArrayList<TileImageFormat> list = new ArrayList<>();
		for (TileImageFormat format : values()) {
			if (tileImageType.equals(format.getType())) {
				list.add(format);
			}
		}
		TileImageFormat[] result = new TileImageFormat[0];
		result = list.toArray(result);
		return result;
	}

	@Override
	public String toString() {
		return displayText;
	}

	public TileImageDataWriterBuilder getDataWriterBuilder() {
		return dataWriterBuilder;
	}

	public TileImageType getType() {
		return dataWriterBuilder.getType();
	}

	/**
	 * File extension
	 *
	 * @return
	 */
	public String getFileExt() {
		return dataWriterBuilder.getType().getFileExt();
	}

}
