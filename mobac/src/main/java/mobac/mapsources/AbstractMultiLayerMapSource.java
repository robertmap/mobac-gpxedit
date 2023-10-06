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

import jakarta.xml.bind.annotation.XmlTransient;
import mobac.exceptions.MapSourceInitializationException;
import mobac.exceptions.TileException;
import mobac.gui.mapview.PreviewMap;
import mobac.program.interfaces.InitializableMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractMultiLayerMapSource implements InitializableMapSource, Iterable<MapSource> {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	protected String name = "";
	protected TileImageType tileType = TileImageType.PNG;
	protected MapSource[] mapSources;

	protected int maxZoom;
	protected int minZoom;
	protected MapSpace mapSpace;
	protected MapSourceLoaderInfo loaderInfo = null;

	public AbstractMultiLayerMapSource(String name, TileImageType tileImageType) {
		this.name = name;
		this.tileType = tileImageType;
	}

	protected AbstractMultiLayerMapSource() {
	}

	protected void initializeValues() {
		MapSource refMapSource = mapSources[0];
		mapSpace = refMapSource.getMapSpace();
		maxZoom = PreviewMap.MAX_ZOOM;
		minZoom = 0;
		for (MapSource ms : mapSources) {
			maxZoom = Math.min(maxZoom, ms.getMaxZoom());
			minZoom = Math.max(minZoom, ms.getMinZoom());
			if (!ms.getMapSpace().equals(mapSpace)) {
				throw new RuntimeException("Different map spaces used in multi-layer map source");
			}
		}
	}

	@Override
	public void initialize() throws MapSourceInitializationException {
		MapSource refMapSource = mapSources[0];
		mapSpace = refMapSource.getMapSpace();
		maxZoom = PreviewMap.MAX_ZOOM;
		minZoom = 0;
		for (MapSource ms : mapSources) {
			if (ms instanceof InitializableMapSource) {
				((InitializableMapSource) ms).initialize();
			}
			maxZoom = Math.min(maxZoom, ms.getMaxZoom());
			minZoom = Math.max(minZoom, ms.getMinZoom());
		}
	}

	public MapSource[] getLayerMapSources() {
		return mapSources;
	}

	public Color getBackgroundColor() {
		return Color.BLACK;
	}

	public MapSpace getMapSpace() {
		return mapSpace;
	}

	public int getMaxZoom() {
		return maxZoom;
	}

	public int getMinZoom() {
		return minZoom;
	}

	public String getName() {
		return name;
	}

	public String getStoreName() {
		return null;
	}

	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, InterruptedException, TileException {
		BufferedImage image = getTileImage(zoom, x, y, loadMethod);
		if (image == null) {
			return null;
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream(16000);
		if (!ImageIO.write(image, tileType.getFileExt(), buf)) {
			throw new IOException(
					String.format("Failed to write image %d/%d/z%d type %s", x, y, zoom, tileType.getFileExt()));
		}
		return buf.toByteArray();
	}

	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, InterruptedException, TileException {
		Graphics2D g2 = null;
		try {
			List<BufferedImage> layerImages = new ArrayList<>(mapSources.length);
			int maxSize = mapSpace.getTileSize();
			for (MapSource layerMapSource : mapSources) {
				BufferedImage layerImage = layerMapSource.getTileImage(zoom, x, y, loadMethod);
				layerImages.add(layerImage); // we add even null values to make sure the layer number is correct
				if (layerImage != null) {
					log.debug("Multi layer image loaded: {} {} {} {}", layerMapSource, x, y, zoom);
					int size = layerImage.getWidth();
					if (size > maxSize) {
						maxSize = size;
					}
				}
			}
			int type;
			if (tileType == TileImageType.PNG || tileType == TileImageType.GIF) {
				// PNG supports alpha transparency
				// GIF supports simple transparency - does not cause problems
				type = BufferedImage.TYPE_4BYTE_ABGR;
			} else {
				type = BufferedImage.TYPE_3BYTE_BGR;
			}
			BufferedImage image = new BufferedImage(maxSize, maxSize, type);
			g2 = image.createGraphics();
			g2.setColor(getBackgroundColor());
			g2.fillRect(0, 0, maxSize, maxSize);

			for (int i = 0; i < layerImages.size(); i++) {
				BufferedImage layerImage = layerImages.get(i);
				if (layerImage != null) {
					float alpha = getLayerAlpha(i);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
					g2.drawImage(layerImage, 0, 0, maxSize, maxSize, null);
				}
			}
			return image;
		} finally {
			if (g2 != null) {
				g2.dispose();
			}
		}
	}

	protected float getLayerAlpha(int layerIndex) {
		return 1.0f;
	}

	public TileImageType getTileImageType() {
		return tileType;
	}

	@Override
	public String toString() {
		return getName();
	}

	public Iterator<MapSource> iterator() {
		return Arrays.asList(mapSources).iterator();
	}

	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		if (this.loaderInfo != null) {
			throw new RuntimeException("LoaderInfo already set");
		}
		this.loaderInfo = loaderInfo;
		for (MapSource ms : mapSources) {
			ms.setLoaderInfo(loaderInfo);
		}
	}

}
