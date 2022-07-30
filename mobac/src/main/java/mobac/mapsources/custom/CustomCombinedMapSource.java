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
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mobac.exceptions.MapSourceInitializationException;
import mobac.exceptions.TileException;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.InitializableMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

@XmlRootElement(name = "combined")
public class CustomCombinedMapSource implements InitializableMapSource {

	@XmlElementWrapper(name = "regionalMapSource")
	@XmlElements({@XmlElement(name = "mapSource", type = StandardMapSourceLayer.class),
			@XmlElement(name = "localTileSQLite", type = CustomLocalTileSQliteMapSource.class),
			@XmlElement(name = "localTileFiles", type = CustomLocalTileFilesMapSource.class),
			@XmlElement(name = "localTileZip", type = CustomLocalTileZipMapSource.class),
			@XmlElement(name = "localImageFile", type = CustomLocalImageFileMapSource.class)})
	protected ArrayList<MapSource> regionalMapSource = new ArrayList<MapSource>();

	@XmlElementWrapper(name = "baseMapSource")
	@XmlElements({@XmlElement(name = "customMapSource", type = CustomMapSource.class),
			@XmlElement(name = "customWmsMapSource", type = CustomWmsMapSource.class),
			@XmlElement(name = "mapSource", type = StandardMapSourceLayer.class),
			@XmlElement(name = "mapsforge", type = CustomMapsforge.class),
			@XmlElement(name = "localTileSQLite", type = CustomLocalTileSQliteMapSource.class),
			@XmlElement(name = "localTileFiles", type = CustomLocalTileFilesMapSource.class),
			@XmlElement(name = "localTileZip", type = CustomLocalTileZipMapSource.class),
			@XmlElement(name = "localImageFile", type = CustomLocalImageFileMapSource.class)})
	protected ArrayList<MapSource> baseMapSource = new ArrayList<MapSource>();

	@XmlElement
	protected String name;

	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	protected Color backgroundColor = Color.BLACK;

	@XmlElement
	protected TileImageType tileType = TileImageType.PNG;

	public CustomCombinedMapSource() {
	}

	@Override
	public void initialize() throws MapSourceInitializationException {
		if (regionalMapSource.size() == 0) {
			throw new MapSourceInitializationException("Regional map missing");
		}
		if (baseMapSource.size() == 0) {
			throw new MapSourceInitializationException("Base map missing");
		}
		if ((regionalMapSource.size() > 1) || baseMapSource.size() > 1) {
			throw new MapSourceInitializationException(
					"Invalid map source definition: multiple regional or base maps defined.");
		}
		if (!(regionalMapSource instanceof FileBasedMapSource)) {
			throw new MapSourceInitializationException(
					"Invalid regional map file format. Only file based local maps are supported!");
		}
		((InitializableMapSource) regionalMapSource).initialize();
		if (baseMapSource instanceof InitializableMapSource) {
			((InitializableMapSource) baseMapSource).initialize();
		}
	}

	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, TileException, InterruptedException {
		try {
			byte[] data = regionalMapSource.get(0).getTileData(zoom, x, y, loadMethod);
			if (data != null) {
				return data;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return baseMapSource.get(0).getTileData(zoom, x, y, loadMethod);
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, TileException, InterruptedException {
		try {
			BufferedImage image = regionalMapSource.get(0).getTileImage(zoom, x, y, loadMethod);
			if (image != null) {
				return image;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return baseMapSource.get(0).getTileImage(zoom, x, y, loadMethod);
	}

	@Override
	public TileImageType getTileImageType() {
		return tileType;
	}

	@Override
	public int getMaxZoom() {
		return baseMapSource.get(0).getMaxZoom();
	}

	@Override
	public int getMinZoom() {
		return baseMapSource.get(0).getMinZoom();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public MapSpace getMapSpace() {
		return baseMapSource.get(0).getMapSpace();
	}

	@Override
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	@Override
	public MapSourceLoaderInfo getLoaderInfo() {
		return null;
	}

	@Override
	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
	}

}
