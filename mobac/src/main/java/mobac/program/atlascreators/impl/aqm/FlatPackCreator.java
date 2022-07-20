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
package mobac.program.atlascreators.impl.aqm;

import mobac.utilities.Utilities;
import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class FlatPackCreator {
    public static final String FLAT_PACK_HEADER = "FLATPACK1";
    public static final int FILE_COPY_BUFFER_LEN = 32768;

    private String packPath = null;

    private OutputStream dataStream = null;

    private ByteArrayOutputStream structBuffer = null;

    private OutputStreamWriter structBufferWriter = null;

    private long currentDataWritedSize = 0;

    private long currentNbFiles = 0;

    public FlatPackCreator(final File packPath) throws FileNotFoundException {
        this(packPath.getAbsolutePath());
    }

    public FlatPackCreator(final String packPath) throws FileNotFoundException {
        this.packPath = packPath;

        if (packPath == null) {
            throw new NullPointerException("Pack file path is null.");
        }

        dataStream = new BufferedOutputStream(new FileOutputStream(packPath + ".tmp"), 16384);
        structBuffer = new ByteArrayOutputStream();
        structBufferWriter = new OutputStreamWriter(structBuffer, StandardCharsets.ISO_8859_1);

        currentDataWritedSize = 0;
        currentNbFiles = 0;
    }

    public final void add(final String filePath, final String fileEntryName) throws IOException {
        add(new File(filePath), fileEntryName);
    }

    public final void add(final File filePath) throws IOException {
        add(filePath, filePath.getName());
    }

    public final void add(final File filePath, final String fileEntryName) throws IOException {
        // read file content

        byte[] buff = FileUtils.readFileToByteArray(filePath);
        add(buff, fileEntryName);
    }

    public final void add(final byte[] buff, final String fileEntryName) throws IOException {
        if (dataStream == null) {
            throw new IOException("Write stream is null.");
        }

        // write file size
        String fileSize = Integer.toString(buff.length) + "\0";
        dataStream.write(fileSize.getBytes(StandardCharsets.ISO_8859_1));

        // write file into pack data
        if (buff.length > 0) {
            dataStream.write(buff);
        }

        // write file into pack structure
        structBufferWriter.append(fileEntryName + "\0" + currentDataWritedSize + "\0");

        // update written size
        currentDataWritedSize += buff.length + fileSize.length();
        currentNbFiles++;
    }

    public final void close() throws IOException {
        if (dataStream == null) {
            throw new NullPointerException("Write stream is null.");
        }

        // close data file
        dataStream.flush();
        dataStream.close();
        dataStream = null;

        File tmpFile = new File(packPath + ".tmp");
        // open pack file
        try (OutputStream packStream = new BufferedOutputStream(new FileOutputStream(packPath))) {
            String nbFiles = Long.toString(currentNbFiles) + "\0";

            // write header
            packStream.write(FLAT_PACK_HEADER.getBytes(StandardCharsets.ISO_8859_1));

            // write struct
            structBufferWriter.flush();
            structBufferWriter.close();
            int headerSize = structBuffer.size() + nbFiles.length();
            packStream.write(Integer.toString(headerSize).getBytes(StandardCharsets.ISO_8859_1));
            packStream.write('\0');

            packStream.write(nbFiles.getBytes(StandardCharsets.ISO_8859_1));

            structBuffer.writeTo(packStream);

            // Free memory
            structBufferWriter = null;
            structBuffer = null;

            // write data
            try (FileInputStream in = new FileInputStream(tmpFile)) {
                byte[] buffer = new byte[FILE_COPY_BUFFER_LEN];

                int read;
                while ((read = in.read(buffer)) > 0) {
                    packStream.write(buffer, 0, read);
                }

            }
            packStream.flush();
        }

        // delete temp file
        if (tmpFile.isFile()) {
            Utilities.deleteFile(tmpFile);
        }

        // reset state
        packPath = null;
        structBuffer = null;
    }

}
