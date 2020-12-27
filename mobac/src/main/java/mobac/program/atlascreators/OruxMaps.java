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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.program.atlascreators;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapCreationException;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.annotations.AtlasCreatorName;
import mobac.program.annotations.SupportedParameters;
import mobac.program.atlascreators.impl.MapTileBuilder;
import mobac.program.atlascreators.impl.MapTileWriter;
import mobac.program.atlascreators.tileprovider.CacheTileProvider;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.interfaces.LayerInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.TileImageFormat;
import mobac.program.model.TileImageParameters;
import mobac.program.model.TileImageParameters.Name;
import mobac.utilities.Utilities;

/**
 * Creates maps using the OruxMaps (Android) atlas format.
 *
 * @author orux
 */
@AtlasCreatorName("OruxMaps")
@SupportedParameters(names = { Name.format })
public class OruxMaps extends AtlasCreator {

	// Calibration file extension
	protected static final String ORUXMAPS_EXT = ".otrk2.xml";

	// OruxMaps tile size
	protected static final int TILE_SIZE = 512;

	protected String calVersionCode;

	// OruxMaps background color
	protected static final Color BG_COLOR = new Color(0xcb, 0xd3, 0xf3);

	// Each layer is a Main map for OruxMaps
	protected File oruxMapsMainDir;

	// Each map is a Layer map for OruxMaps
	protected File oruxMapsLayerDir;

	// Images directory for each map
	protected File oruxMapsImagesDir;

	protected LayerInterface currentLayer;

	// We need to override the map name, All maps must have the same prefix (layer name)
	protected String mapName;

	public OruxMaps() {
		super();
		calVersionCode = "2.1";
	}

	@Override
	public boolean testMapSource(MapSource mapSource) {

		return (mapSource.getMapSpace() instanceof MercatorPower2MapSpace);
	}

	@Override
	protected void testAtlas() throws AtlasTestException {

		for (LayerInterface layer : atlas) {
			int cont = layer.getMapCount();
			for (int i = 0; i < cont; i++) {
				MapInterface currMap = layer.getMap(i);
				int currZoomLevel = currMap.getZoom();
				for (int j = i + 1; j < cont; j++) {
					MapInterface nextMap = layer.getMap(j);
					int nextZoomLevel = nextMap.getZoom();
					if (currZoomLevel == nextZoomLevel)
						throw new AtlasTestException(
								"Unable to create a map with more than a layer with the same zoom level: " + currMap
										+ " & " + nextMap + "\nPossible causes:\n"
										+ "You are combining several layers (using drag & drop in 'Atlas Content')\n"
										+ "You are creating a large map, and you have not selected the maximum value in 'Settings - Map size'");
				}
			}
		}
	}

	/*
	 * @see mobac.program.atlascreators.AtlasCreator#initLayerCreation(mobac.program .interfaces.LayerInterface)
	 */
	@Override
	public void initLayerCreation(LayerInterface layer) throws IOException {

		super.initLayerCreation(layer);
		currentLayer = layer;
		oruxMapsMainDir = new File(atlasDir, layer.getName());
		Utilities.mkDirs(oruxMapsMainDir);

	}

	@Override
	public void finishLayerCreation() throws IOException {

		super.finishLayerCreation();
		writeMainOtrk2File(currentLayer.getName());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see mobac.program.atlascreators.AtlasCreator#initializeMap(mobac.program. interfaces.MapInterface,
	 * mobac.utilities.tar.TarIndex)
	 */
	@Override
	public void initializeMap(MapInterface map, TileProvider mapTileProvider) {

		super.initializeMap(map, mapTileProvider);
		// OruxMaps default image format, jpeg90; always TILE_SIZE=512;
		if (parameters == null)
			parameters = new TileImageParameters(TILE_SIZE, TILE_SIZE, TileImageFormat.JPEG90);
		else
			parameters = new TileImageParameters(TILE_SIZE, TILE_SIZE, parameters.getFormat());
		mapName = String.format("%s %02d", currentLayer.getName(), map.getZoom());
	}

	@Override
	public void createMap() throws MapCreationException, InterruptedException {

		oruxMapsLayerDir = new File(oruxMapsMainDir, mapName);
		oruxMapsImagesDir = new File(oruxMapsLayerDir, "set");
		try {
			Utilities.mkDirs(oruxMapsImagesDir);
			writeOtrk2File();
			createTiles();
		} catch (InterruptedException e) {
			// User has aborted process
			return;
		} catch (Exception e) {
			throw new MapCreationException(map, e);
		}
	}

	protected void createTiles() throws InterruptedException, MapCreationException {

		CacheTileProvider ctp = new CacheTileProvider(mapDlTileProvider);
		try {
			mapDlTileProvider = ctp;

			OruxMapTileBuilder mapTileBuilder = new OruxMapTileBuilder(this, new OruxMapTileWriter());
			atlasProgress.initMapCreation(mapTileBuilder.getCustomTileCount());
			mapTileBuilder.createTiles();
		} finally {
			ctp.cleanup();
		}
	}

	/**
	 * Main calibration file
	 *
	 * @param name
	 */
	private void writeMainOtrk2File(String name) {

		File otrk2 = new File(oruxMapsMainDir, name + ORUXMAPS_EXT);

		StringWriter sw = new StringWriter();
		sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sw.append("<OruxTracker " + "xmlns=\"http://oruxtracker.com/app/res/calibration\"\n" + " versionCode=\""
				+ calVersionCode + "\">\n");
		sw.append("<MapCalibration layers=\"true\" layerLevel=\"0\">\n");
		sw.append("<MapName><![CDATA[" + name + "]]></MapName>\n");

		sw.append(appendMapContent());

		sw.append("</MapCalibration>\n");
		sw.append("</OruxTracker>\n");

		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(otrk2), StandardCharsets.UTF_8)) {
			writer.write(sw.toString());
		} catch (IOException e) {
			log.error("", e);
		}
	}

	protected String appendMapContent() {
		return "";
	}

	/**
	 * Main calibration file per layer
	 *
	 */
	protected void writeOtrk2File() {
		File otrk2File = new File(oruxMapsLayerDir, mapName + ORUXMAPS_EXT);
		String data = prepareOtrk2File();
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(otrk2File),
				StandardCharsets.UTF_8)) {
			writer.write(data);
		} catch (IOException e) {
			log.error("", e);
		}
	}

	/**
	 * Main calibration file per layer
	 *
	 */
	protected String prepareOtrk2File() {

		StringBuilder mapWriter = new StringBuilder();
		MapSpace mapSpace = mapSource.getMapSpace();
		double longitudeMin = mapSpace.cXToLon(xMin * tileSize, zoom);
		double longitudeMax = mapSpace.cXToLon((xMax + 1) * tileSize, zoom);
		double latitudeMin = mapSpace.cYToLat((yMax + 1) * tileSize, zoom);
		double latitudeMax = mapSpace.cYToLat(yMin * tileSize, zoom);
		mapWriter.append(
				"<OruxTracker " + "xmlns=\"http://oruxtracker.com/app/res/calibration\"\n" + " versionCode=\"2.1\">\n");
		mapWriter.append("<MapCalibration layers=\"false\" layerLevel=\"" + map.getZoom() + "\">\n");
		mapWriter.append("<MapName><![CDATA[" + mapName + "]]></MapName>\n");

		// convert ampersands and others
		String mapFileName = mapName;
		mapFileName = mapFileName.replaceAll("&", "&amp;");
		mapFileName = mapFileName.replaceAll("<", "&lt;");
		mapFileName = mapFileName.replaceAll(">", "&gt;");
		mapFileName = mapFileName.replaceAll("\"", "&quot;");
		mapFileName = mapFileName.replaceAll("'", "&apos;");

		int mapWidth = (xMax - xMin + 1) * tileSize;
		int mapHeight = (yMax - yMin + 1) * tileSize;
		int numXimg = (mapWidth + TILE_SIZE - 1) / TILE_SIZE;
		int numYimg = (mapHeight + TILE_SIZE - 1) / TILE_SIZE;
		mapWriter.append("<MapChunks xMax=\"" + numXimg + "\" yMax=\"" + numYimg + "\" datum=\"" + "WGS84"
				+ "\" projection=\"" + "Mercator" + "\" img_height=\"" + TILE_SIZE + "\" img_width=\"" + TILE_SIZE
				+ "\" file_name=\"" + mapFileName + "\" />\n");
		mapWriter.append("<MapDimensions height=\"" + mapHeight + "\" width=\"" + mapWidth + "\" />\n");
		mapWriter.append("<MapBounds minLat=\"" + latitudeMin + "\" maxLat=\"" + latitudeMax + "\" minLon=\""
				+ longitudeMin + "\" maxLon=\"" + longitudeMax + "\" />\n");
		mapWriter.append("<CalibrationPoints>\n");
		String cb = "<CalibrationPoint corner=\"%s\" lon=\"%2.6f\" lat=\"%2.6f\" />\n";
		mapWriter.append(String.format(Locale.ENGLISH, cb, "TL", longitudeMin, latitudeMax));
		mapWriter.append(String.format(Locale.ENGLISH, cb, "BR", longitudeMax, latitudeMin));
		mapWriter.append(String.format(Locale.ENGLISH, cb, "TR", longitudeMax, latitudeMax));
		mapWriter.append(String.format(Locale.ENGLISH, cb, "BL", longitudeMin, latitudeMin));
		mapWriter.append("</CalibrationPoints>\n");
		mapWriter.append("</MapCalibration>\n");
		mapWriter.append("</OruxTracker>\n");
		return mapWriter.toString();
	}

	protected class OruxMapTileBuilder extends MapTileBuilder {

		public OruxMapTileBuilder(AtlasCreator atlasCreator, MapTileWriter mapTileWriter) {
			super(atlasCreator, mapTileWriter, false);
		}

		@Override
		protected void prepareTile(Graphics2D graphics) {
			graphics.setColor(BG_COLOR);
			graphics.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
		}

	}

	private class OruxMapTileWriter implements MapTileWriter {

		public void writeTile(int tilex, int tiley, String tileType, byte[] tileData) throws IOException {
			String tileFileName = String.format("%s_%d_%d.omc2", mapName, tilex, tiley);
			File tileFile = new File(oruxMapsImagesDir, tileFileName);
			try (FileOutputStream out = new FileOutputStream(tileFile)) {
				out.write(tileData);
			}
		}

		public void finalizeMap() {
			// Nothing to do
		}

	}
}
