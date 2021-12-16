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
package mobac.utilities.tar;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A tar record contains the data of the file to be stored. The data size has to be dividable by 512 (internal tar block
 * length).
 */
public class TarRecord {

    private final byte[] fileData;

    public TarRecord(File theFile) throws IOException {
        fileData = new byte[calculateFileSizeInTar(theFile)];
        this.setRecordContent(theFile);
    }

    public TarRecord(byte[] data) throws IOException {
        this(data, 0, data.length);
    }

    public TarRecord(byte[] data, int off, int len) throws IOException {
        fileData = new byte[calculateFileSizeInTar(len)];
        System.arraycopy(data, off, fileData, 0, len);
    }

    public static int calculateFileSizeInTar(File theFile) {
        long fl = theFile.length();
        if (fl > Integer.MAX_VALUE)
            throw new RuntimeException("File size too large");
        return calculateFileSizeInTar((int) fl);
    }

    public static int calculateFileSizeInTar(int fileLength) {
        if (fileLength < 512) {
            return 512;
        } else {
            int mod = fileLength % 512;
            // align buffer size on 512 byte block length
            if (mod != 0)
                fileLength += 512 - mod;
            return fileLength;
        }
    }

    public byte[] getRecordContent() {
        return fileData;
    }

    public void setRecordContent(File theFile) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(theFile))) {
            in.readFully(fileData, 0, (int) theFile.length());
        }
    }
}
