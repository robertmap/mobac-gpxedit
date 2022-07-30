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
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mobac.exceptions.MapSourceInitializationException;
import mobac.mapsources.AbstractMultiLayerMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.ReloadableMapSource;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.TileImageType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlSeeAlso({CustomMapSource.class})
public class CustomMultiLayerMapSource extends AbstractMultiLayerMapSource
		implements
			ReloadableMapSource<CustomMultiLayerMapSource> {

	@XmlElementWrapper(name = "layers")
	@XmlElements({@XmlElement(name = "customMapSource", type = CustomMapSource.class),
			@XmlElement(name = "customWmsMapSource", type = CustomWmsMapSource.class),
			@XmlElement(name = "mapSource", type = StandardMapSourceLayer.class),
			@XmlElement(name = "mapsforge", type = CustomMapsforge.class),
			@XmlElement(name = "localTileSQLite", type = CustomLocalTileSQliteMapSource.class),
			@XmlElement(name = "localTileFiles", type = CustomLocalTileFilesMapSource.class),
			@XmlElement(name = "localTileZip", type = CustomLocalTileZipMapSource.class),
			@XmlElement(name = "localImageFile", type = CustomLocalImageFileMapSource.class)})
	protected List<CustomMapSource> layers = new ArrayList<>();

	@XmlList()
	protected List<Float> layersAlpha = new ArrayList<>();

	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	protected Color backgroundColor = Color.BLACK;

	public CustomMultiLayerMapSource() {
		super();
		mapSources = new MapSource[0];
	}

	@Override
	public void applyChangesFrom(CustomMultiLayerMapSource reloadedMapSource) throws MapSourceInitializationException {
		if (!name.equals(reloadedMapSource.getName())) {
			throw new MapSourceInitializationException("The map name has changed");
		}
		this.layers = reloadedMapSource.layers;
		this.layersAlpha = reloadedMapSource.layersAlpha;
		this.backgroundColor = reloadedMapSource.backgroundColor;
		this.tileType = reloadedMapSource.tileType;
		this.mapSources = reloadedMapSource.mapSources;
		this.maxZoom = reloadedMapSource.maxZoom;
		this.minZoom = reloadedMapSource.minZoom;
	}

	public TileImageType getTileType() {
		return tileType;
	}

	public void setTileType(TileImageType tileType) {
		this.tileType = tileType;
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		mapSources = new MapSource[layers.size()];
		layers.toArray(mapSources);
		initializeValues();
	}

	@XmlElement(name = "name")
	public String getMLName() {
		return name;
	}

	public void setMLName(String name) {
		this.name = name;
	}

	@Override
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	@Override
	protected float getLayerAlpha(int layerIndex) {
		if (layersAlpha.size() <= layerIndex) {
			return 1.0f;
		}

		return layersAlpha.get(layerIndex);
	}
}
