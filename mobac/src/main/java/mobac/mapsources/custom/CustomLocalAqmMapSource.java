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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mobac.exceptions.MapSourceInitializationException;
import mobac.exceptions.TileException;
import mobac.mapsources.custom.aqm.AqmMap;
import mobac.mapsources.mapspace.MapSpaceFactory;
import mobac.program.interfaces.InitializableMapSource;
import mobac.program.interfaces.MapSourceInitialDisplayPosition;
import mobac.program.interfaces.MapSpace;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;
import mobac.utilities.I18nUtils;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Alpine Quest Map : http://alpinequest.net/ Developer :
 * ph-t@users.sourceforge.net
 */

@XmlRootElement(name = "localAQMfile")
public class CustomLocalAqmMapSource implements InitializableMapSource, MapSourceInitialDisplayPosition {

	private final MapSpace mapSpace = MapSpaceFactory.getInstance(256, true); // todo créer avec les données AQM
	@XmlElement(nillable = false, defaultValue = "CustomLocalAQMfile")
	private String name = "CustomLocalAQMfile";
	private int minZoom;
	private int maxZoom;
	@XmlElement(required = true)
	private File sourceFile = null;
	@XmlElement(required = false)
	private TileImageType tileImageType = TileImageType.JPG;
	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	private Color backgroundColor = Color.BLACK;

	private MapSourceLoaderInfo loaderInfo = null; // todo créer avec les données AQM

	private AqmMap map;

	public CustomLocalAqmMapSource() {
		super();
	}

	public int getInitialDisplayPositionX() {
		if (map == null) {
			return 0;
		}
		return map.getMinZoomXtCenter();
	}

	public int getInitialDisplayPositionY() {
		if (map == null) {
			return 0;
		}
		return map.getMinZoomYtCenter();
	}

	@Override
	public int getMaxZoom() {
		return maxZoom;
	}

	@Override
	public int getMinZoom() {
		return minZoom;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException {

		long longNbTotalTiles = Math.round(Math.pow(2, zoom));
		int intNbTotalTiles = Math.toIntExact(longNbTotalTiles);
		int inverted_y = intNbTotalTiles - y;

		return map.getByteTile(zoom, x, inverted_y);
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, TileException, InterruptedException {
		byte[] bTile = getTileData(zoom, x, y, loadMethod);
		if (bTile == null) {
			return null;
		}
		return ImageIO.read(new ByteArrayInputStream(bTile));
	}

	@Override
	public TileImageType getTileImageType() {
		return tileImageType;
	}

	@Override
	public MapSpace getMapSpace() {
		return mapSpace;
	}

	@Override
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	@Override
	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	@Override
	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		this.loaderInfo = loaderInfo;
	}

	@Override
	public void initialize() throws MapSourceInitializationException {
		if (!sourceFile.isFile()) {
			JOptionPane.showMessageDialog(null,
					String.format(I18nUtils.localizedStringForKey("msg_custom_map_invalid_source_aqm"), name,
							sourceFile),
					I18nUtils.localizedStringForKey("msg_custom_map_invalid_source_file_title"),
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			this.map = new AqmMap(sourceFile);
		} catch (IOException e) {
			throw new MapSourceInitializationException(e);
		}
		this.minZoom = map.minZoom;
		this.maxZoom = map.maxZoom;
		this.tileImageType = TileImageType.getTileImageType(map.imgFormat);
	}

}
