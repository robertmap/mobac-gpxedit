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
package mobac.utilities;

import mobac.Main;
import mobac.exceptions.MOBACOutOfMemoryException;
import mobac.program.interfaces.MapSource;
import mobac.program.model.TileImageType;
import mobac.utilities.file.DirOrFileExtFilter;
import mobac.utilities.file.DirectoryFileFilter;
import mobac.utilities.imageio.ImageFormatDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    public static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);
    public static final DecimalFormatSymbols DFS_ENG = new DecimalFormatSymbols(Locale.ENGLISH);
    public static final DecimalFormatSymbols DFS_LOCAL = new DecimalFormatSymbols();
    public static final DecimalFormat FORMAT_6_DEC = new DecimalFormat("#0.######");
    public static final DecimalFormat FORMAT_6_DEC_ENG = new DecimalFormat("#0.######", DFS_ENG);
    public static final DecimalFormat FORMAT_2_DEC = new DecimalFormat("0.00");
    public static final long SECONDS_PER_HOUR = TimeUnit.HOURS.toSeconds(1);
    public static final long SECONDS_PER_DAY = TimeUnit.DAYS.toSeconds(1);
    private static final DecimalFormat cDmsMinuteFormatter = new DecimalFormat("00");
    private static final DecimalFormat cDmsSecondFormatter = new DecimalFormat("00.0");
    private static final Logger log = LoggerFactory.getLogger(Utilities.class);

    public static boolean testJaiColorQuantizerAvailable() {
        try {
            Class<?> c = Class.forName("javax.media.jai.operator.ColorQuantizerDescriptor");
            return true;
        } catch (NoClassDefFoundError e) {
            return false;
        } catch (Throwable t) {
            log.error("Error in testJaiColorQuantizerAvailable():", t);
            return false;
        }
    }

    public static BufferedImage createEmptyTileImage(MapSource mapSource) {
        int tileSize = mapSource.getMapSpace().getTileSize();
        Color color = mapSource.getBackgroundColor();

        int imageType;
        if (color.getAlpha() == 255) {
            imageType = BufferedImage.TYPE_INT_RGB;
        } else {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage emptyImage = new BufferedImage(tileSize, tileSize, imageType);
        Graphics2D g = emptyImage.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(0, 0, tileSize, tileSize);
        } finally {
            g.dispose();
        }
        return emptyImage;
    }

    public static BufferedImage safeCreateBufferedImage(int width, int height, int imageType) {
        try {
            return new BufferedImage(width, height, imageType);
        } catch (NegativeArraySizeException e) {
            String message = String.format("Image size is too large: " + "%dx%d pixels", width, height);
            throw new RuntimeException(message);
        } catch (OutOfMemoryError e) {
            int bytesPerPixel = getBytesPerPixel(imageType);
            if (bytesPerPixel < 0) {
                throw e;
            }
            long requiredMemory = ((long) width) * ((long) height) * bytesPerPixel;
            String message = String.format("Available free memory not sufficient for creating image of size %dx%d pixels", width, height);
            throw new MOBACOutOfMemoryException(requiredMemory, message);
        }
    }

    /**
     * @param bufferedImageType as used for {@link BufferedImage#BufferedImage(int, int, int)}
     * @return
     */
    public static int getBytesPerPixel(int bufferedImageType) {
        switch (bufferedImageType) {
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return 4;
            case BufferedImage.TYPE_3BYTE_BGR:
                return 3;
            case BufferedImage.TYPE_USHORT_GRAY:
            case BufferedImage.TYPE_USHORT_565_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
                return 2;
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_BYTE_INDEXED:
                return 1;
        }
        return -1;
    }

    public static byte[] createEmptyTileData(MapSource mapSource) {
        BufferedImage emptyImage = createEmptyTileImage(mapSource);
        ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
        try {
            TileImageType tileImageType = mapSource.getTileImageType();
            if (!ImageIO.write(emptyImage, tileImageType.getFileExt(), buf)) {
                throw new IOException(String.format("Failed to create empty tile of type %s", tileImageType.getFileExt()));
            }
            return buf.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Kept for compatibility reasons (e.g. used by tilestore-util)
     *
     * @param imageData
     * @return
     */
    public static TileImageType getImageType(byte[] imageData) {
        return ImageFormatDetector.getImageType(imageData);
    }

    public static boolean startsWith(byte[] data, byte[] sequenceToTest) {
        return startsWith(data, 0, sequenceToTest);
    }

    /**
     * @param data           where we search
     * @param offset         offset in <code>data</code> where to start searching for the <code>sequenceToTest</code>
     * @param sequenceToTest what we search
     * @return
     */
    public static boolean startsWith(byte[] data, int offset, byte[] sequenceToTest) {
        int end = offset + sequenceToTest.length;
        if (data.length < end) {
            return false;
        }

        for (int i = 0; i < sequenceToTest.length; i++) {
            if (data[i + offset] != sequenceToTest[i]) {
                return false;
            }
        }
        return true;
    }

    public static InputStream loadResourceAsStream(String resourcePath) throws IOException {
        return Main.class.getResourceAsStream("resources/" + resourcePath);
    }

    public static String loadTextResource(String resourcePath) throws IOException {
        resourcePath = "resources/" + resourcePath;
        try (InputStream in = Main.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource does not exist: " + resourcePath);
            }
            byte[] buf = in.readAllBytes();
            return new String(buf, StandardCharsets.UTF_8);
        }
    }

    /**
     * @param imageName imagePath resource path relative to the class {@link Main}
     * @return
     */
    public static ImageIcon loadResourceImageIcon(String imageName) {
        URL url = Main.class.getResource("resources/images/" + imageName);
        return new ImageIcon(url);
    }

    public static URL getResourceImageUrl(String imageName) {
        return Main.class.getResource("resources/images/" + imageName);
    }

    public static void loadProperties(Properties p, URL url) throws IOException {
        try (InputStream propIn = url.openStream()) {
            p.load(propIn);
        }
    }

    public static void loadProperties(Properties p, File f) throws IOException {
        try (InputStream propIn = new FileInputStream(f)) {
            p.load(propIn);
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the current {@link Thread} has been interrupted and if so a {@link InterruptedException}. Therefore it
     * behaves similar to {@link Thread#sleep(long)} without actually slowing down anything by sleeping a certain amount
     * of time.
     *
     * @throws InterruptedException
     */
    public static void checkForInterruption() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    /**
     * Checks if the current {@link Thread} has been interrupted and if so a {@link RuntimeException} will be thrown.
     * This method is useful for long lasting operations that do not allow to throw an {@link InterruptedException}.
     *
     * @throws RuntimeException
     */
    public static void checkForInterruptionRt() throws RuntimeException {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException(new InterruptedException());
        }
    }

    public static void closeQuietly(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
        }
    }

    public static double parseLocaleDouble(String text) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Number n = Utilities.FORMAT_6_DEC.parse(text, pos);
        if (n == null) {
            throw new ParseException("Unknown error", 0);
        }
        if (pos.getIndex() != text.length())
            throw new ParseException("Text ends with unparsable characters", pos.getIndex());
        return n.doubleValue();
    }

    public static void showTooltipNow(JComponent c) {
        Action toolTipAction = c.getActionMap().get("postTip");
        if (toolTipAction != null) {
            ActionEvent postTip = new ActionEvent(c, ActionEvent.ACTION_PERFORMED, "");
            toolTipAction.actionPerformed(postTip);
        }
    }

    /**
     * Formats a byte value depending on the size to "Bytes", "KiBytes", "MiByte" and "GiByte"
     *
     * @param bytes
     * @return Formatted {@link String}
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1000) {
            return Long.toString(bytes) + " " + I18nUtils.localizedStringForKey("Bytes");
        }
        if (bytes < 1000000) {
            return FORMAT_2_DEC.format(bytes / 1024d) + " " + I18nUtils.localizedStringForKey("KiByte");
        }
        if (bytes < 1000000000) {
            return FORMAT_2_DEC.format(bytes / 1048576d) + " " + I18nUtils.localizedStringForKey("MiByte");
        }
        return FORMAT_2_DEC.format(bytes / 1073741824d) + " " + I18nUtils.localizedStringForKey("GiByte");
    }

    public static String formatDurationSeconds(long seconds) {
        long x = seconds;
        long days = x / SECONDS_PER_DAY;
        x %= SECONDS_PER_DAY;
        int years = (int) (days / 365);
        days -= (years * 365);

        int months = (int) (days * 12d / 365d);
        String m = (months == 1) ? "month" : "months";

        if (years > 5) {
            return String.format("%d years", years);
        }
        if (years > 0) {
            String y = (years == 1) ? "year" : "years";
            return String.format("%d %s %d %s", years, y, months, m);
        }
        String d = (days == 1) ? "day" : "days";
        if (months > 0) {
            days -= months * (365d / 12d);
            return String.format("%d %s %d %s", months, m, days, d);
        }
        long hours = TimeUnit.SECONDS.toHours(x);
        String h = (hours == 1) ? "hour" : "hours";
        x -= hours * SECONDS_PER_HOUR;
        if (days > 0) {
            return String.format("%d %s %d %s", days, d, hours, h);
        }
        long minutes = TimeUnit.SECONDS.toMinutes(x);
        String min = (minutes == 1) ? "minute" : "minutes";
        if (hours > 0) {
            return String.format("%d %s %d %s", hours, h, minutes, min);
        }
        return String.format("%d %s", minutes, min);
    }

    public static void mkDir(File dir) throws IOException {
        if (dir.isDirectory()) {
            return;
        }
        if (!dir.mkdir()) {
            throw new IOException("Failed to create directory \"" + dir.getAbsolutePath() + "\"");
        }
    }

    public static void mkDirs(File dir) throws IOException {
        if (dir.isDirectory()) {
            return;
        }

        if (dir.mkdirs()) {
            return;
        }

//        if (Logging.isCONFIGURED()) {
//            Logging.LOG.error("mkDirs creation failed first time - one retry left");
//        }

        // Wait some time and then retry it.
        // See for details:
        // http://javabyexample.wisdomplug.com/component/content/article/37-core-java/48-is-mkdirs-thread-safe.html
        // Hopefully this will fix the different bugs reported for this method
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        if (dir.mkdirs()) {
            return;
        }

        throw new IOException("Failed to create directory \"" + dir.getAbsolutePath() + "\"");
    }

    public static void fileCopy(File sourceFile, File destFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                try (FileChannel source = fis.getChannel()) {
                    try (FileChannel destination = fos.getChannel()) {
                        destination.transferFrom(source, 0, source.size());
                    }
                }
            }
        }
    }

    public static byte[] getFileBytes(File file) throws IOException {
        int size = (int) file.length();
        byte[] buffer = new byte[size];
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            in.readFully(buffer);
            return buffer;
        }
    }

    /**
     * Fully reads data from <tt>in</tt> to an internal buffer until the end of in has been reached. Then the buffer is
     * returned.
     *
     * @param in data source to be read
     * @return buffer all data available in in
     * @throws IOException
     */
    public static byte[] getInputBytes(InputStream in) throws IOException {
        int initialBufferSize = in.available();
        if (initialBufferSize <= 0) {
            initialBufferSize = 32768;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(initialBufferSize);
        byte[] b = new byte[4096];
        int ret = 0;
        while ((ret = in.read(b)) >= 0) {
            buffer.write(b, 0, ret);
        }
        return buffer.toByteArray();
    }

    /**
     * Fully reads data from <tt>in</tt> the read data is discarded.
     *
     * @param in
     * @throws IOException
     */
    public static void readFully(InputStream in) throws IOException {
        byte[] b = new byte[4096];
        while ((in.read(b)) >= 0) {
        }
    }

    /**
     * Lists all direct sub directories of <code>dir</code>
     *
     * @param dir
     * @return list of directories
     */
    public static File[] listSubDirectories(File dir) {
        return dir.listFiles(new DirectoryFileFilter());
    }

    public static List<File> listSubDirectoriesRec(File dir, int maxDepth) {
        List<File> dirList = new LinkedList<File>();
        addSubDirectories(dirList, dir, maxDepth);
        return dirList;
    }

    public static void addSubDirectories(List<File> dirList, File dir, int maxDepth) {
        File[] subDirs = dir.listFiles(new DirectoryFileFilter());
        for (File f : subDirs) {
            dirList.add(f);
            if (maxDepth > 0) {
                addSubDirectories(dirList, f, maxDepth - 1);
            }
        }
    }

    public static String prettyPrintLatLon(double coord, boolean isCoordKindLat) {
        boolean neg = coord < 0.0;
        String c;
        if (isCoordKindLat) {
            c = (neg ? "S" : "N");
        } else {
            c = (neg ? "W" : "E");
        }
        double tAbsCoord = Math.abs(coord);
        int tDegree = (int) tAbsCoord;
        double tTmpMinutes = (tAbsCoord - tDegree) * 60;
        int tMinutes = (int) tTmpMinutes;
        double tSeconds = (tTmpMinutes - tMinutes) * 60;
        return c + tDegree + "\u00B0" + cDmsMinuteFormatter.format(tMinutes) + "\'" + cDmsSecondFormatter.format(tSeconds) + "\"";
    }

    public static void setHttpProxyHost(String host) {
        if (host != null && host.length() > 0) {
            System.setProperty("http.proxyHost", host);
        } else {
            System.getProperties().remove("http.proxyHost");
        }
    }

    public static void setHttpProxyPort(String port) {
        if (port != null && port.length() > 0) {
            System.setProperty("http.proxyPort", port);
        } else {
            System.getProperties().remove("http.proxyPort");
        }
    }

    /**
     * Returns the file path for the selected class. If the class is located inside a JAR file the return if the JAR file.
     * If the class file is executed outside of an JAR the root directory holding the class/package structure is returned.
     *
     * @param mainClass
     * @return
     */
    public static Path getClassLocation(Class<?> mainClass) {
        ProtectionDomain pDomain = mainClass.getProtectionDomain();
        CodeSource cSource = pDomain.getCodeSource();
        try {
            URL loc = cSource.getLocation();
            return Paths.get(loc.toURI());
        } catch (Exception e) {
            throw new RuntimeException("Unable to determine program directory: ", e);
        }
    }

    /**
     * Tries to delete a file or directory and throws an {@link IOException} if that fails.
     *
     * @param fileToDelete
     * @throws IOException Thrown if <code>fileToDelete</code> can not be deleted.
     */
    public static void deleteFile(File fileToDelete) throws IOException {
        if (!fileToDelete.delete()) {
            throw new IOException("Deleting of \"" + fileToDelete + "\" failed.");
        }
    }

    public static void renameFile(File oldFile, File newFile) throws IOException {
        if (!oldFile.renameTo(newFile)) {
            throw new IOException("Failed to rename file: " + oldFile + " to " + newFile);
        }
    }

    public static int getJavaMaxHeapMB() {
        try {
            return (int) (Runtime.getRuntime().maxMemory() / 1048576l);
        } catch (Exception e) {
            return -1;
        }
    }

    public static byte[] downloadHttpFile(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new IOException("Invalid HTTP response: " + responseCode + " for url " + conn.getURL());
        try (InputStream in = conn.getInputStream()) {
            return Utilities.getInputBytes(in);
        }
    }

    public static void copyFile(File source, File target) throws IOException {
        try (FileInputStream in = new FileInputStream(source)) {
            try (FileOutputStream out = new FileOutputStream(target)) {
                in.getChannel().transferTo(0, source.length(), out.getChannel());
            }
        }
    }

    /**
     * @param value positive value
     * @return 0 if no bit is set else the highest bit that is one in <code>value</code>
     */
    public static int getHighestBitSet(int value) {
        int bit = 0x40000000;
        for (int i = 31; i > 0; i--) {
            int test = bit & value;
            if (test != 0) {
                return i;
            }
            bit >>= 1;
        }
        return 0;
    }

    /**
     * @param revision SVN revision string like <code>"1223"</code>, <code>"1224M"</code> or <code>"1616:1622M"</code>
     * @return parsed svn revision
     */
    public static int parseSVNRevision(String revision) {
        revision = revision.trim();
        int index = revision.lastIndexOf(':');
        if (index >= 0) {
            revision = revision.substring(index + 1).trim();
        }
        Matcher m = Pattern.compile("(\\d+)[^\\d]*").matcher(revision);
        if (!m.matches()) {
            return -1;
        }
        return Integer.parseInt(m.group(1));
    }

    /**
     * This method recursively traverse a folder and all its descendants, applying to them a FileExtFilter.
     * DirOrFileExtFilter was used, to don't filter out the folders, when traversing them. But in the end, only files
     * will be returned
     *
     * @param dirOrFile         - file or directory that will be traversed and analyzed
     * @param dirOFileExtFilter - filter that will pass-through all folders and files only with requested extension
     * @return a list of files with requested extension
     * @author Maksym "elmuSSo" Kondej
     */
    public static List<File> traverseFolder(File dirOrFile, DirOrFileExtFilter dirOFileExtFilter) {
        ArrayList<File> result = new ArrayList<File>();
        if (dirOrFile.isDirectory()) {
            File[] allFiles = dirOrFile.listFiles(dirOFileExtFilter);
            for (File innerFile : allFiles) {
                result.addAll(traverseFolder(innerFile, dirOFileExtFilter));
            }
        } else if (dirOrFile.isFile()) {
            // Only files are added to the results list
            result.add(dirOrFile);
        }
        return result;
    }

    /**
     * Searches for the file specified by the relative <code>fileName</code> in all supplied directories. Returns the
     * first existing (isFile() == <code>true</code>) combination of fileName and searchDir or <code>null</code> in case
     * no match was found.
     * <p>
     * In case <code>fileName</code> is an absolute path it will be returned as File in case it exists.
     *
     * @param fileName
     * @param searchDirs
     * @return
     */
    public static File findFile(String fileName, File... searchDirs) {
        File f = new File(fileName);
        if (f.isAbsolute()) {
            if (f.isFile()) {
                return f;
            } else {
                return null;
            }
        }
        for (File dir : searchDirs) {
            if (dir == null) {
                continue; // ignore null entries
            }
            log.debug("Searching for file \"" + fileName + "\" in directory \"" + dir + "\"");
            if (new File(dir, fileName).isFile()) {
                return f;
            }
        }
        return null;
    }
}
