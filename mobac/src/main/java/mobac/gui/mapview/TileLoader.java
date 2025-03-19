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
package mobac.gui.mapview;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import mobac.exceptions.DownloadFailedException;
import mobac.gui.mapview.Tile.TileState;
import mobac.gui.mapview.interfaces.TileLoaderListener;
import mobac.program.download.TileDownLoader;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSource.LoadMethod;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.TileStoreEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Loads tiles from OSM via HTTP and saves all loaded files in a directory
 * located in the temporary directory. If a tile is present in this file cache
 * it will not be loaded from OSM again.
 *
 * @author Jan Peter Stotz
 * @author r_x
 */
public class TileLoader {

	private static final Logger log = LoggerFactory.getLogger(TileLoader.class);

	protected TileStore tileStore;
	protected TileLoaderListener listener;

	public TileLoader(TileLoaderListener listener) {
		super();
		this.listener = listener;
		tileStore = TileStore.getInstance();
	}

	public Runnable createTileLoaderJob(final MapSource source, final int tilex, final int tiley, final int zoom) {
		return new TileAsyncLoadJob(source, tilex, tiley, zoom);
	}

	protected class TileAsyncLoadJob implements Runnable {

		final int tilex, tiley, zoom;
		final MapSource mapSource;
		protected TileStoreEntry tileStoreEntry = null;
		Tile tile;
		boolean fileTilePainted = false;

		public TileAsyncLoadJob(MapSource source, int tilex, int tiley, int zoom) {
			super();
			this.mapSource = source;
			this.tilex = tilex;
			this.tiley = tiley;
			this.zoom = zoom;
		}

		public void run() {
			final MemoryTileCache cache = listener.getTileImageCache();
			synchronized (cache) {
				tile = cache.getTile(mapSource, tilex, tiley, zoom);
				if (tile == null || tile.tileState != TileState.TS_NEW) {
					return;
				}
				tile.setTileState(TileState.TS_LOADING);
			}
			if (loadTileFromStore()) {
				return;
			}
			if (fileTilePainted) {
				Runnable job = () -> loadOrUpdateTile();
				JobDispatcher.getInstance().addJob(job);
			} else {
				loadOrUpdateTile();
			}
		}

		protected void loadOrUpdateTile() {
			try {
				BufferedImage image = mapSource.getTileImage(zoom, tilex, tiley, LoadMethod.DEFAULT);
				if (image != null) {
					tile.setImage(image);
					tile.setTileState(TileState.TS_LOADED);
					listener.tileLoadingFinished(tile, true);
				} else {
					tile.setErrorImage();
					listener.tileLoadingFinished(tile, false);
				}
				return;
			} catch (SSLHandshakeException e) {
				log.warn("SSL/TLS error prevented download of {}: {}", tile, e.getMessage());
				tile.setErrorImage();
				tile.setErrorMessage("TLS error: " + e.getMessage());
			} catch (DownloadFailedException e) {
				log.warn("Downloading of {} failed: {}", tile, e.getMessage());
				if (e.isTypeImage()) {
					tile.setErrorImage(e.getResponseData());
				} else {
					tile.setErrorImage();
					tile.setErrorMessage(e.generateResponseErrorText());
				}
			} catch (IOException e) {
				if (log.isTraceEnabled()) {
					log.warn("Downloading of {} failed: {}", tile, e.getMessage(), e);
				} else {
					log.warn("Downloading of {} failed: {}", tile, e.getMessage());
				}
				tile.setErrorImage();
				tile.setErrorMessage(e.getClass().getSimpleName() + "\n" + e.getMessage());
			} catch (Exception e) {
				log.debug("Downloading of {} failed", tile, e);
				tile.setErrorImage();
				tile.setErrorMessage(e.getClass().getSimpleName() + "\n" + e.getMessage());
			}
			listener.tileLoadingFinished(tile, false);
		}

		protected boolean loadTileFromStore() {
			try {
				BufferedImage image = mapSource.getTileImage(zoom, tilex, tiley, LoadMethod.CACHE);
				if (image == null) {
					return false;
				}
				tile.setImage(image);
				listener.tileLoadingFinished(tile, true);
				if (TileDownLoader.isTileExpired(tileStoreEntry)) {
					return false;
				}
				fileTilePainted = true;
				return true;
			} catch (Exception e) {
				log.error("Failed to load tile (z={},x={},y={}) from tile store", zoom, tilex, tiley, e);
			}
			return false;
		}

	}

}
