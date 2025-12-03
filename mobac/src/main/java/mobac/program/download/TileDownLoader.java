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
package mobac.program.download;

import mobac.exceptions.DownloadFailedException;
import mobac.exceptions.UnrecoverableDownloadException;
import mobac.program.interfaces.HttpMapSource;
import mobac.program.interfaces.MapSourceListener;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.Settings;
import mobac.program.model.TileImageType;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.TileStoreEntry;
import mobac.utilities.Utilities;
import mobac.utilities.imageio.ImageFormatDetector;
import mobac.utilities.stream.ThrottledInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class TileDownLoader {

	private static final Logger log = LoggerFactory.getLogger(TileDownLoader.class);

	static {
		Object defaultReadTimeout = System.getProperty("sun.net.client.defaultReadTimeout");
		if (defaultReadTimeout == null) {
			System.setProperty("sun.net.client.defaultReadTimeout", "15000");
		}
		System.setProperty("http.maxConnections", "20");

		// Disable restricted headers: By default some headers can't be set
		// see https://stackoverflow.com/questions/8335501
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}

	public static void init() {
		// This method is empty. It is just for enforcing execution of the static code
		// block above
	}

	public static byte[] getImage(int x, int y, int zoom, HttpMapSource mapSource)
			throws IOException, InterruptedException, UnrecoverableDownloadException {

		MapSpace mapSpace = mapSource.getMapSpace();
		int maxTileIndex = mapSpace.getMaxPixels(zoom) / mapSpace.getTileSize();
		if (x > maxTileIndex) {
			throw new RuntimeException("Invalid tile index x=" + x + " for zoom " + zoom);
		}
		if (y > maxTileIndex) {
			throw new RuntimeException("Invalid tile index y=" + y + " for zoom " + zoom);
		}

		TileStore ts = TileStore.getInstance();

		// Thread.sleep(2000);

		// Test code for creating random download failures
		// if (Math.random()>0.7) throw new
		// IOException("intentionally download error");

		TileStoreEntry tile = null;
		if (Settings.getInstance().tileStoreEnabled) {

			// Copy the file from the persistent tilestore instead of
			// downloading it from internet.
			tile = ts.getTile(x, y, zoom, mapSource);
			boolean expired = isTileExpired(tile);
			if (tile != null) {
				if (expired) {
					log.trace("Expired: {} {}", mapSource.getName(), tile);
				} else {
					log.trace("Tile of map source {} used from tilestore", mapSource.getName());
					byte[] data = tile.getData();
					notifyCachedTileUsed(data.length);
					return data;
				}
			}
		}
		byte[] data;
		if (tile == null) {
			data = downloadTileAndUpdateStore(x, y, zoom, mapSource);
			notifyTileDownloaded(data.length);
		} else {
			byte[] updatedData = updateStoredTile(tile, mapSource);
			if (updatedData != null) {
				data = updatedData;
				notifyTileDownloaded(data.length);
			} else {
				data = tile.getData();
				notifyCachedTileUsed(data.length);
			}
		}
		return data;
	}

	private static void notifyTileDownloaded(int size) {
		if (Thread.currentThread() instanceof MapSourceListener) {
			((MapSourceListener) Thread.currentThread()).tileDownloaded(size);
		}

	}

	private static void notifyCachedTileUsed(int size) {
		if (Thread.currentThread() instanceof MapSourceListener) {
			((MapSourceListener) Thread.currentThread()).tileLoadedFromCache(size);
		}
	}

	/**
	 * Download the tile from the web server and updates the tile store if the tile
	 * could be successfully retrieved.
	 *
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @return
	 * @throws UnrecoverableDownloadException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static byte[] downloadTileAndUpdateStore(int x, int y, int zoom, HttpMapSource mapSource)
			throws UnrecoverableDownloadException, IOException, InterruptedException {
		return downloadTileAndUpdateStore(x, y, zoom, mapSource, Settings.getInstance().tileStoreEnabled);
	}

	public static byte[] downloadTile(int x, int y, int zoom, HttpMapSource mapSource)
			throws UnrecoverableDownloadException, IOException, InterruptedException {
		return downloadTileAndUpdateStore(x, y, zoom, mapSource, false);
	}

	public static byte[] downloadTileAndUpdateStore(int x, int y, int zoom, HttpMapSource mapSource,
			boolean useTileStore) throws UnrecoverableDownloadException, IOException, InterruptedException {

		if (zoom < 0) {
			throw new UnrecoverableDownloadException("Negative zoom!");
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(zoom, x, y);
		if (conn == null) {
			throw new UnrecoverableDownloadException(
					"Tile x=" + x + " y=" + y + " zoom=" + zoom + " is not a valid tile in map source " + mapSource);
		}

		log.trace("Downloading {}", conn.getURL());

		mapSource.prepareConnection(conn);
		conn.connect();

		byte[] data = loadBodyDataInBuffer(conn);
		int code = conn.getResponseCode();

		if (code != HttpURLConnection.HTTP_OK) {
			throw new DownloadFailedException(conn, data);
		}

		checkContentType(conn, data);
		checkContentLength(conn, data);

		String eTag = conn.getHeaderField("ETag");
		long timeLastModified = conn.getLastModified();
		long timeExpires = conn.getExpiration();

		Utilities.checkForInterruption();
		TileImageType imageType = ImageFormatDetector.getImageType(data);
		if (imageType == null) {
			throw new UnrecoverableDownloadException("The returned image is of unknown format");
		}
		if (useTileStore) {
			TileStore.getInstance().putTileData(data, x, y, zoom, mapSource, timeLastModified, timeExpires, eTag);
		}
		Utilities.checkForInterruption();
		return data;
	}

	public static byte[] updateStoredTile(TileStoreEntry tile, HttpMapSource mapSource)
			throws UnrecoverableDownloadException, IOException, InterruptedException {
		final int x = tile.getX();
		final int y = tile.getY();
		final int zoom = tile.getZoom();
		final HttpMapSource.TileUpdate tileUpdate = mapSource.getTileUpdate();

		switch (tileUpdate) {
			case ETag : {
				boolean unchanged = hasTileETag(tile, mapSource);
				if (unchanged) {
					log.trace("Data unchanged on server (eTag): {} {}", mapSource, tile);
					return null;
				}
				break;
			}
			case LastModified : {
				boolean isNewer = isTileNewer(tile, mapSource);
				if (!isNewer) {
					log.trace("Data unchanged on server (LastModified): {} {}", mapSource, tile);
					return null;
				}
				break;
			}
			default :
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(zoom, x, y);
		if (conn == null) {
			throw new UnrecoverableDownloadException(
					"Tile x=" + x + " y=" + y + " zoom=" + zoom + " is not a valid tile in map source " + mapSource);
		}
		log.trace("Checking {} {}", mapSource.getName(), tile);

		mapSource.prepareConnection(conn);

		boolean conditionalRequest = false;

		switch (tileUpdate) {
			case IfNoneMatch : {
				if (tile.geteTag() != null) {
					conn.setRequestProperty("If-None-Match", tile.geteTag());
					conditionalRequest = true;
				}
				break;
			}
			case IfModifiedSince : {
				if (tile.getTimeLastModified() > 0) {
					conn.setIfModifiedSince(tile.getTimeLastModified());
					conditionalRequest = true;
				}
				break;
			}
			default :
		}

		conn.connect();

		int code = conn.getResponseCode();

		if (conditionalRequest && code == HttpURLConnection.HTTP_NOT_MODIFIED) {
			// Data unchanged on server
			if (Settings.getInstance().tileStoreEnabled) {
				tile.update(conn.getExpiration());
				TileStore.getInstance().putTile(tile, mapSource);
			}
			if (log.isTraceEnabled()) {
				log.trace("Data unchanged on server: {} {}", mapSource, tile);
			}
			return null;
		}
		byte[] data = loadBodyDataInBuffer(conn);

		if (code != HttpURLConnection.HTTP_OK) {
			throw new DownloadFailedException(conn, data);
		}

		checkContentType(conn, data);
		checkContentLength(conn, data);

		String eTag = conn.getHeaderField("ETag");
		long timeLastModified = conn.getLastModified();
		long timeExpires = conn.getExpiration();

		Utilities.checkForInterruption();
		TileImageType imageType = ImageFormatDetector.getImageType(data);
		if (imageType == null) {
			throw new UnrecoverableDownloadException("The returned image is of unknown format");
		}
		if (Settings.getInstance().tileStoreEnabled) {
			TileStore.getInstance().putTileData(data, x, y, zoom, mapSource, timeLastModified, timeExpires, eTag);
		}
		Utilities.checkForInterruption();
		return data;
	}

	public static boolean isTileExpired(TileStoreEntry tileStoreEntry) {
		if (tileStoreEntry == null) {
			return true;
		}
		long expiredTime = tileStoreEntry.getTimeExpires();
		if (expiredTime > 0) {
			// server had set an expiration time
			long maxExpirationTime = Settings.getInstance().tileMaxExpirationTime + tileStoreEntry.getTimeDownloaded();
			long minExpirationTime = Settings.getInstance().tileMinExpirationTime + tileStoreEntry.getTimeDownloaded();
			expiredTime = Math.max(minExpirationTime, Math.min(maxExpirationTime, expiredTime));
		} else {
			// no expiration time set by server - use the default one
			expiredTime = tileStoreEntry.getTimeDownloaded() + Settings.getInstance().tileDefaultExpirationTime;
		}
		// the tile has expired if the expiration time has passed / is in the past
		return (expiredTime < System.currentTimeMillis());
	}

	/**
	 * Reads all available data from the input stream of <code>conn</code> and
	 * returns it as byte array. If no input data is available the method returns
	 * <code>null</code>.
	 *
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	protected static byte[] loadBodyDataInBuffer(HttpURLConnection conn) throws IOException {
		byte[] data = null;
		try (InputStream input = conn.getInputStream()) {
			InputStream wrappedInput = input;
			if (Thread.currentThread() instanceof MapSourceListener) {
				// We only throttle atlas downloads, not downloads for the preview map
				long bandwidthLimit = Settings.getInstance().getBandwidthLimit();
				if (bandwidthLimit > 0) {
					wrappedInput = new ThrottledInputStream(input);
				}
			}
			data = Utilities.getInputBytes(wrappedInput);
		} catch (IOException e) {
			byte[] errData;
			try (InputStream errorIn = conn.getErrorStream()) {
				errData = Utilities.getInputBytes(errorIn);
				log.trace("Retrieved {} error bytes for a HTTP {} from {}", errData.length, conn.getResponseCode(),
						conn.getURL());

			} catch (Exception e1) {
				log.debug("Error retrieving error stream content: " + e1);
				errData = null;
			}
			throw new DownloadFailedException(conn, errData, e);
		}
		log.trace("Retrieved {} bytes for a HTTP {}", data.length, conn.getResponseCode());
		if (data.length == 0) {
			return null;
		}
		return data;
	}

	/**
	 * Performs a <code>HEAD</code> request for retrieving the
	 * <code>LastModified</code> header value.
	 */
	protected static boolean isTileNewer(TileStoreEntry tile, HttpMapSource mapSource) throws IOException {
		long oldLastModified = tile.getTimeLastModified();
		if (oldLastModified <= 0) {
			log.warn("Tile age comparison not possible: tile in tilestore does not contain lastModified attribute");
			return true;
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(tile.getZoom(), tile.getX(), tile.getY());
		conn.setRequestMethod("HEAD");
		conn.setRequestProperty("Accept", Settings.getInstance().getHttpAccept());
		long newLastModified = conn.getLastModified();
		if (newLastModified == 0) {
			return true;
		}
		return (newLastModified > oldLastModified);
	}

	protected static boolean hasTileETag(TileStoreEntry tile, HttpMapSource mapSource) throws IOException {
		String eTag = tile.geteTag();
		if (eTag == null || eTag.isEmpty()) {
			log.warn("ETag check not possible: tile in tilestore does not contain ETag attribute");
			return true;
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(tile.getZoom(), tile.getX(), tile.getY());
		conn.setRequestMethod("HEAD");
		conn.setRequestProperty("Accept", Settings.getInstance().getHttpAccept());
		String onlineETag = conn.getHeaderField("ETag");
		if (onlineETag == null || onlineETag.isEmpty()) {
			return true;
		}
		return (onlineETag.equals(eTag));
	}

	protected static void checkContentType(HttpURLConnection conn, byte[] data)
			throws DownloadFailedException, IOException {
		String contentType = conn.getContentType();
		if (contentType != null) {
			contentType = contentType.toLowerCase();
			if (!contentType.startsWith("image/")) {
				if (log.isTraceEnabled() && contentType.startsWith("text/")) {
					log.trace("Content (" + contentType + "): " + new String(data));
				}
				throw new DownloadFailedException(conn, data);
			}
		}
	}

	/**
	 * Check if the retrieved data length is equal to the header value
	 * Content-Length
	 *
	 * @param conn
	 * @param data
	 * @throws UnrecoverableDownloadException
	 */
	protected static void checkContentLength(HttpURLConnection conn, byte[] data)
			throws UnrecoverableDownloadException {
		int len = conn.getContentLength();
		if (len < 0) {
			return;
		}
		if (data.length != len) {
			throw new UnrecoverableDownloadException("Content length is not as declared by the server: retrived="
					+ data.length + " bytes  expected-content-length=" + len + " bytes");
		}
	}
}
