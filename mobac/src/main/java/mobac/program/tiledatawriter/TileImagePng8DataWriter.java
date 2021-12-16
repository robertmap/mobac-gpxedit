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

import mobac.optional.JavaAdvancedImaging;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public class TileImagePng8DataWriter extends TileImagePngDataWriter {

    public TileImagePng8DataWriter() {
        super();
    }

    @Override
    public void processImage(BufferedImage image, OutputStream out) throws IOException {
        BufferedImage image2 = JavaAdvancedImaging.colorReduceMedianCut(image, 256);
        super.processImage(image2, out);
    }

}
