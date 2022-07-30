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
package mobac.program.atlascreators.tileprovider;

import mobac.program.interfaces.MapSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A tile cache with speculative loading on a separate thread. Usually this
 * decreases map generation time on multi-core systems.
 */
public class CacheTileProvider implements TileProvider {

	/**
	 * Counter for identifying the different threads
	 */
	private static int PRELOADER_THREAD_NUM = 1;
	protected final TileProvider tileProvider;
	private final Logger log = LoggerFactory.getLogger(CacheTileProvider.class);
	private final Map<CacheKey, SRCachedTile> cache = new ConcurrentHashMap<>(500);
	private PreLoadThread preLoader = new PreLoadThread();

	public CacheTileProvider(TileProvider tileProvider) {
		this.tileProvider = tileProvider;
		preLoader.start();
	}

	public boolean preferTileImageUsage() {
		return true;
	}

	public BufferedImage getTileImage(int x, int y) throws IOException {
		SRCachedTile cachedTile = cache.get(new CacheKey(x, y));
		BufferedImage image = null;
		if (cachedTile != null) {
			CachedTile tile = cachedTile.get();
			if (tile != null) {
				if (tile.loaded) {
					log.trace("Cache hit: x={} y={}", x, y);
				}
				image = tile.getImage();
				if (!tile.nextLoadJobCreated) {
					// log.debug(String.format("Preload job added : x=%d y=%d l=%d",
					// x + 1, y, layer));
					preloadTile(new CachedTile(new CacheKey(x + 1, y)));
					tile.nextLoadJobCreated = true;
				}
			}
		}
		if (image == null) {
			log.trace("Cache miss: x={}} y={}}", x, y);
			// log.debug(String.format("Preload job added : x=%d y=%d l=%d", x +
			// 1, y, layer));
			preloadTile(new CachedTile(new CacheKey(x + 1, y)));
			image = internalGetTileImage(x, y);
		}
		return image;
	}

	protected BufferedImage internalGetTileImage(int x, int y) throws IOException {
		synchronized (tileProvider) {
			return tileProvider.getTileImage(x, y);
		}
	}

	public byte[] getTileData(int layer, int x, int y) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	public byte[] getTileData(int x, int y) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	public MapSource getMapSource() {
		return tileProvider.getMapSource();
	}

	private void preloadTile(CachedTile tile) {
		if (preLoader.queue.remainingCapacity() < 1) {
			// Preloader thread is too slow
			log.trace("Preloading rejected: {}", tile.key);
			return;
		}
		if (cache.get(tile.key) != null) {
			return;
		}
		try {
			preLoader.queue.add(tile);
			cache.put(tile.key, new SRCachedTile(tile));
		} catch (IllegalStateException e) {
			// Queue is "full"
			log.trace("Preloading rejected: {}", tile.key);
		}
	}

	public void cleanup() {
		try {
			cache.clear();
			if (preLoader != null) {
				preLoader.interrupt();
				preLoader = null;
			}
		} catch (Throwable t) {
			log.error("", t);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void finalize() throws Throwable {
		cleanup();
		super.finalize();
	}

	private static class SRCachedTile extends SoftReference<CachedTile> {

		public SRCachedTile(CachedTile referent) {
			super(referent);
		}

	}

	private static class CacheKey {
		int x;
		int y;

		public CacheKey(int x, int y) {
			super();
			this.x = x;
			this.y = y;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CacheKey other = (CacheKey) obj;
			if (x != other.x) {
				return false;
			}
			return y == other.y;
		}

		@Override
		public String toString() {
			return "CacheKey [x=" + x + ", y=" + y + "]";
		}

	}

	private class PreLoadThread extends Thread {

		/**
		 * pre-loading more than 20 tiles doesn't make much sense
		 */
		private final LinkedBlockingQueue<CachedTile> queue = new LinkedBlockingQueue<>(20);

		public PreLoadThread() {
			super("ImagePreLoadThread" + (PRELOADER_THREAD_NUM++));
			setDaemon(true);
		}

		@Override
		public void run() {
			log.debug("Image pre-loader thread started");
			CachedTile tile;
			try {
				while (true) {
					tile = queue.take();
					if (tile != null && !tile.loaded) {
						// log.trace("Loading image async: " + tile);
						tile.loadImage();
					}
				}
			} catch (InterruptedException e) {
				log.debug("Image pre-loader thread terminated");
			}
		}

	}

	private class CachedTile {

		final CacheKey key;
		boolean loaded = false;
		boolean nextLoadJobCreated = false;
		private BufferedImage image;
		private IOException loadException = null;

		public CachedTile(CacheKey key) {
			super();
			this.key = key;
			image = null;
		}

		public synchronized void loadImage() {
			try {
				image = internalGetTileImage(key.x, key.y);
			} catch (IOException e) {
				loadException = e;
			} catch (Exception e) {
				loadException = new IOException(e);
			}
			loaded = true;
		}

		public synchronized BufferedImage getImage() throws IOException {
			if (!loaded) {
				loadImage();
			}
			if (loadException != null) {
				throw loadException;
			}
			return image;
		}

		@Override
		public String toString() {
			return "CachedTile [key=" + key + ", loaded=" + loaded + ", nextLoadJobCreated=" + nextLoadJobCreated + "]";
		}

	}
}
