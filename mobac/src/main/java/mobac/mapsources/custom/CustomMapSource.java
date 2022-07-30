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
package mobac.mapsources.custom;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mobac.exceptions.MapSourceInitializationException;
import mobac.exceptions.UnrecoverableDownloadException;
import mobac.mapsources.AbstractHttpMapSourceBase;
import mobac.mapsources.MapSourceTools;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.download.MobacSSLHelper;
import mobac.program.download.TileDownLoader;
import mobac.program.interfaces.MapSourceListener;
import mobac.program.interfaces.MapSpace;
import mobac.program.interfaces.ReloadableMapSource;
import mobac.program.jaxb.BooleanAdapter;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.TileStoreEntry;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLSocketFactory;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom tile store provider, configurable via settings.xml.
 */
@XmlRootElement
public class CustomMapSource extends AbstractHttpMapSourceBase implements ReloadableMapSource<CustomMapSource> {

	/**
	 * List of trusted public key (hex encoded lowercase SHA-256 hash of the encoded
	 * public key)
	 */
	@XmlElementWrapper(name = "trustedPublicKeys")
	@XmlElement(name = "publicKeyHash")
	public final Set<String> trustedPublicKeys = new HashSet<>();
	@XmlElement(defaultValue = "PNG")
	protected TileImageType tileType = TileImageType.PNG;
	@XmlElement(required = true, nillable = false)
	protected String url = "http://127.0.0.1/{$x}_{$y}_{$z}";
	@XmlElement(nillable = false, defaultValue = "Custom")
	private String name = "Custom";
	@XmlElement(defaultValue = "0")
	private int minZoom = 0;
	@XmlElement(required = true)
	private int maxZoom = 0;
	@XmlElement(defaultValue = "NONE")
	private TileUpdate tileUpdate = TileUpdate.None;
	@XmlElement(defaultValue = "false")
	@XmlJavaTypeAdapter(value = BooleanAdapter.class, type = boolean.class)
	private boolean invertYCoordinate = false;
	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	private Color backgroundColor = Color.BLACK;
	@XmlElement(required = false, defaultValue = "false")
	@XmlJavaTypeAdapter(value = BooleanAdapter.class, type = boolean.class)
	private boolean ignoreErrors = false;
	@XmlElement(required = false, defaultValue = "")
	@XmlList
	private String[] serverParts = null;
	private int currentServerPart = 0;
	private SSLSocketFactory sslSocketFactory = SSL_SOCKET_FACTORY;

	private MapSourceLoaderInfo loaderInfo = null;

	/**
	 * Constructor without parameters - required by JAXB
	 */
	protected CustomMapSource() {
	}

	public CustomMapSource(String name, String url) {
		this.name = name;
		this.url = url;
	}

	@Override
	public void applyChangesFrom(CustomMapSource reloadedMapSource) throws MapSourceInitializationException {
		if (!name.equals(reloadedMapSource.getName())) {
			throw new MapSourceInitializationException("The map name has changed");
		}

		minZoom = reloadedMapSource.minZoom;
		maxZoom = reloadedMapSource.maxZoom;
		tileType = reloadedMapSource.tileType;
		tileUpdate = reloadedMapSource.tileUpdate;
		url = reloadedMapSource.url;
		invertYCoordinate = reloadedMapSource.invertYCoordinate;
		backgroundColor = reloadedMapSource.backgroundColor;
		ignoreErrors = reloadedMapSource.ignoreErrors;
		serverParts = reloadedMapSource.serverParts;
		loaderInfo = reloadedMapSource.loaderInfo;
		afterUnmarshal(null, null);
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		if (trustedPublicKeys.size() > 0) {
			sslSocketFactory = MobacSSLHelper.createSSLSocketFactory(trustedPublicKeys);
		}
	}

	public TileUpdate getTileUpdate() {
		return tileUpdate;
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
		return name;
	}

	public TileImageType getTileImageType() {
		return tileType;
	}

	public HttpURLConnection getTileUrlConnection(int zoom, int tilex, int tiley) throws IOException {
		String url = getTileUrl(zoom, tilex, tiley);
		if (url == null) {
			return null;
		}
		return (HttpURLConnection) new URL(url).openConnection();
	}

	public String getTileUrl(int zoom, int tilex, int tiley) {
		if (serverParts == null || serverParts.length == 0) {
			return MapSourceTools.formatMapUrl(url, zoom, tilex, tiley);
		} else {
			currentServerPart = (currentServerPart + 1) % serverParts.length;
			String serverPart = serverParts[currentServerPart];
			return MapSourceTools.formatMapUrl(url, serverPart, zoom, tilex, tiley);
		}
	}

	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, UnrecoverableDownloadException, InterruptedException {

		if (loadMethod == LoadMethod.CACHE) {
			TileStoreEntry entry = TileStore.getInstance().getTile(x, y, zoom, this);
			if (entry == null) {
				return null;
			}
			byte[] data = entry.getData();
			if (Thread.currentThread() instanceof MapSourceListener) {
				((MapSourceListener) Thread.currentThread()).tileDownloaded(data.length);
			}
			return data;
		}
		try {
			if (invertYCoordinate) {
				y = ((1 << zoom) - y - 1);
			}

			return TileDownLoader.getImage(x, y, zoom, this);
		} catch (Exception e) {
			if (ignoreErrors) {
				log.info("Ignored error: " + e);
				return null;
			}
			throw e;
		}
	}

	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, UnrecoverableDownloadException, InterruptedException {

		byte[] data = getTileData(zoom, x, y, loadMethod);

		if (data == null) {
			if (!ignoreErrors) {
				return null;
			}
			int tileSize = this.getMapSpace().getTileSize();
			BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = image.getGraphics();
			try {
				g.setColor(backgroundColor);
				g.fillRect(0, 0, tileSize, tileSize);
			} finally {
				g.dispose();
			}
			return image;

		}
		return ImageIO.read(new ByteArrayInputStream(data));

	}

	@Override
	public String toString() {
		return name;
	}

	public MapSpace getMapSpace() {
		return MercatorPower2MapSpace.INSTANCE_256;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	@Override
	protected SSLSocketFactory getSslSocketFactory() {
		return sslSocketFactory;
	}

	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		this.loaderInfo = loaderInfo;
	}

}
