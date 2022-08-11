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
package mobac.mapsources;

import jakarta.xml.bind.annotation.XmlElement;
import mobac.exceptions.MapSourceInitializationException;
import mobac.exceptions.NotImplementedException;
import mobac.exceptions.UnrecoverableDownloadException;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.interfaces.CloneableMapSource;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.interfaces.RefreshableMapSource;
import mobac.program.model.Atlas;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore.DataPolicy;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapsforgeMapSource implements MapSource, FileBasedMapSource, RefreshableMapSource, CloneableMapSource {

	private static final Logger LOG = LoggerFactory.getLogger(MapsforgeMapSource.class);

	private static final String name = "MapsforgeWorld";

	static {
		Parameters.VALIDATE_COORDINATES = false;
	}

	protected MapSourceLoaderInfo loaderInfo = null;
	protected List<File> mapFileList = new ArrayList<>();
	protected DatabaseRenderer renderer;
	protected XmlRenderTheme xmlRenderTheme;
	protected DataPolicy dataPolicy = MultiMapDataStore.DataPolicy.RETURN_ALL;
	protected DisplayModel displayModel;
	protected MultiMapDataStore multiMapDataStore;
	protected RenderThemeFuture renderThemeFuture;
	protected XmlRenderThemeStyleMenu renderThemeStyleMenu;
	protected MapsForgeCache labelInfoCache = new MapsForgeCache();
	protected TileBasedLabelStore tileBasedLabelStore = new MyTileBasedLabelStore(1000);
	@XmlElement(defaultValue = "false")
	protected boolean transparent = false;
	@XmlElement(defaultValue = "1.0")
	protected float textScale = 1.0f;
	private MapSpace mapSpace = MercatorPower2MapSpace.INSTANCE_256;

	public MapsforgeMapSource() {
		this("world.map");
	}

	public MapsforgeMapSource(String mapFileName) {
		mapFileList.add(new File(mapFileName));
		displayModel = new DisplayModel();
		xmlRenderTheme = InternalRenderTheme.OSMARENDER;
	}

	@Override
	public void initialize() throws MapSourceInitializationException {
		for (File mapFile : mapFileList) {
			if (!mapFile.exists()) {
				throw new MapSourceInitializationException("File does not exist: " + mapFile.getAbsolutePath());
			}
		}
		reinitialize();
	}

	@Override
	public void reinitialize() {
		GraphicFactory graphicFactory = AwtGraphicFactory.INSTANCE;
		multiMapDataStore = new MultiMapDataStore(dataPolicy);
		boolean first = true;
		for (File mapFile : mapFileList) {
			try {
				MapFile mf = new MapFile(mapFile);
				multiMapDataStore.addMapDataStore(mf, first, first);
				first = false;
			} catch (MapFileException e) {
				if (LOG.isTraceEnabled()) {
					LOG.error("Failed to load MapSource file \"{}\": {}", mapFile, e.getMessage(), e);
				} else {
					LOG.error("Failed to load MapSource file \"{}\": {}", mapFile, e.getMessage());
				}
				throw e;
			}
		}
		labelInfoCache.purge();
		tileBasedLabelStore.clear();
		renderer = new DatabaseRenderer(multiMapDataStore, graphicFactory, labelInfoCache, tileBasedLabelStore, true,
				true, null);
		renderThemeFuture = new RenderThemeFuture(graphicFactory, xmlRenderTheme, displayModel);

		// new Thread(renderThemeFuture).start();
		renderThemeFuture.run();
		// renderThemeFuture = new RenderThemeFuture(graphicFactory, xmlRenderTheme,
		// displayModel);
		// renderThemeFuture.run();
	}

	protected void loadExternalRenderTheme(File xmlRenderThemeFile) throws FileNotFoundException {

		XmlRenderThemeMenuCallback callBack = new XmlRenderThemeMenuCallback() {

			@Override
			public Set<String> getCategories(XmlRenderThemeStyleMenu styleMenu) {
				renderThemeStyleMenu = styleMenu;
				String id = styleMenu.getDefaultValue();
				XmlRenderThemeStyleLayer baseLayer = styleMenu.getLayer(id);
				Set<String> result = baseLayer.getCategories();

				for (XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
					LOG.trace("Overlay {} enabled: {}", overlay.getId(), overlay.isEnabled());
					if (overlay.isEnabled()) {
						result.addAll(overlay.getCategories());
					}
				}

				return result;
			}

		};
		this.xmlRenderTheme = new ExternalRenderTheme(xmlRenderThemeFile, callBack);
	}

	@Override
	public void refresh() {
		reinitialize();
	}

	@XmlElement
	protected void setMultiMapDataPolicy(MultiMapDataStore.DataPolicy value) {
		dataPolicy = value;
	}

	public Color getBackgroundColor() {
		return Color.WHITE;
	}

	public MapSpace getMapSpace() {
		return mapSpace;
	}

	public int getMaxZoom() {
		return 19;
	}

	public int getMinZoom() {
		return 0;
	}

	public String getName() {
		return name;
	}

	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, UnrecoverableDownloadException, InterruptedException {
		try (ByteArrayOutputStream buf = new ByteArrayOutputStream(16000)) {
			BufferedImage image = getTileImage(zoom, x, y, loadMethod);
			if (image == null) {
				return null;
			}
			if (!ImageIO.write(image, "png", buf)) {
				throw new RuntimeException("Failed to write PNG image");
			}
			return buf.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException(
					String.format("Failed to render tile {}/{}/z{} - {}", x, y, zoom, e.getMessage()), e);
		}
	}

	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, UnrecoverableDownloadException, InterruptedException {
		if (mapFileList == null || xmlRenderTheme == null || loadMethod == LoadMethod.CACHE) {
			return null;
		}

		// ((MapSourceCallerThreadInfo)Thread.currentThread()).isMapPreviewThread()
		RendererJob job;
		Bitmap tileBitmap;
		Tile tile = new Tile(x, y, (byte) zoom, 256);
		job = new RendererJob(tile, multiMapDataStore, renderThemeFuture, displayModel, textScale, transparent, false);

		// We only need the TileCache for correct label rendering, and it does not
		// actually store the created tile
		// therefore we can create the cache entry before rendering the tile...
		synchronized (renderer) {
			labelInfoCache.put(job, null);
			tileBitmap = renderer.executeJob(job);
		}
		if (tileBitmap == null) {
			LOG.error("Failed to render image {}/{}/z{}", x, y, zoom);
			return null;
		}
		return AwtGraphicFactory.getBitmap(tileBitmap);
	}

	public float getUserScaleFactor() {
		return displayModel.getUserScaleFactor();
	}

	@XmlElement
	public void setUserScaleFactor(float scaleFactor) {
		displayModel.setUserScaleFactor(scaleFactor);
	}

	public TileImageType getTileImageType() {
		return TileImageType.PNG;
	}

	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		this.loaderInfo = loaderInfo;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Clone the Mapforge map source but clear the label cache. This prevents
	 * rendering problems with defect labels.
	 * <p>
	 * This method is executed while creating a deep clone of an {@link Atlas}
	 * (before atlas creation starts).
	 */
	@Override
	public MapsforgeMapSource clone() throws CloneNotSupportedException {
		MapsforgeMapSource mapSource = (MapsforgeMapSource) super.clone();
		mapSource.labelInfoCache = new MapsForgeCache();
		mapSource.tileBasedLabelStore = new MyTileBasedLabelStore(1000);
		reinitialize();
		return mapSource;
	}

	private static class MapsForgeCache implements TileCache {

		HashSet<Integer> set = new HashSet<>(10000);

		public void put(Job job, TileBitmap tile) {
			set.add(job.hashCode());
			// System.out.println("Added: " + job.getKey());
		}

		public boolean containsKey(Job job) {
			return set.contains(job.hashCode());
		}

		public void destroy() {
		}

		public TileBitmap get(Job job) {
			throw new NotImplementedException();
		}

		public int getCapacity() {
			throw new NotImplementedException();
		}

		public int getCapacityFirstLevel() {
			throw new NotImplementedException();
		}

		public TileBitmap getImmediately(Job job) {
			throw new NotImplementedException();
		}

		public void setWorkingSet(Set<Job> jobs) {
			throw new NotImplementedException();
		}

		public void addObserver(Observer observer) {
			throw new NotImplementedException();
		}

		public void removeObserver(Observer observer) {
			throw new NotImplementedException();
		}

		public void purge() {
			set.clear();
		}

	}

	private static class MyTileBasedLabelStore extends TileBasedLabelStore {

		public MyTileBasedLabelStore(int capacity) {
			super(capacity);
		}

		@Override
		public synchronized List<MapElementContainer> getVisibleItems(Tile upperLeft, Tile lowerRight) {
			return super.getVisibleItems(upperLeft, lowerRight);
		}

	}
}
