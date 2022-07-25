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
package mapsources;

import jakarta.xml.bind.JAXBException;
import junit.framework.TestSuite;
import mobac.mapsources.AbstractMultiLayerMapSource;
import mobac.mapsources.DefaultMapSourcesManager;
import mobac.mapsources.MapSourcesManager;
import mobac.mapsources.impl.DebugMapSource;
import mobac.mapsources.impl.LocalhostTestSource;
import mobac.program.ProgramInfo;
import mobac.program.interfaces.HttpMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.EastNorthCoordinate;
import mobac.program.model.Settings;
import mobac.tools.Cities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unittests.helper.DummyTileStore;

import java.net.HttpURLConnection;
import java.util.HashSet;

/**
 * {@link TestSuite} that tests every available map source for operability. The
 * operability test consists of the download of one map tile at the highest
 * available zoom level of the map source. By default the map tile to be
 * downloaded is located in the middle of Berlin. As some map providers do not
 * cover Berlin for each {@link MapSource} a different test coordinate can be
 * specified using testCoordinates.
 */
public class MapSourcesTestSuite extends TestSuite {

	public static final EastNorthCoordinate C_DEFAULT = Cities.BERLIN;
	protected final Logger log;
	private final HashSet<String> testedMapSources;

	public MapSourcesTestSuite() throws JAXBException {
		super();
		HttpURLConnection.setFollowRedirects(false);
		log = LoggerFactory.getLogger(MapSourcesTestSuite.class);
		testedMapSources = new HashSet<String>();
		DefaultMapSourcesManager.initialize();
		Settings.load();
		for (MapSource mapSource : MapSourcesManager.getInstance().getAllMapSources()) {
			if (mapSource instanceof DebugMapSource || mapSource instanceof LocalhostTestSource)
				continue;
			if (mapSource instanceof AbstractMultiLayerMapSource) {
				for (MapSource ms : (AbstractMultiLayerMapSource) mapSource)
					addMapSourcesTestCase(ms);
			} else
				addMapSourcesTestCase(mapSource);
		}
	}

	public static TestSuite suite() throws JAXBException {
		ProgramInfo.initialize(); // Load revision info
		DummyTileStore.initialize();
		DefaultMapSourcesManager.initializeIntelliJMapPacksOnly();
		MapSourcesTestSuite testSuite = new MapSourcesTestSuite();
		return testSuite;
	}

	private void addMapSourcesTestCase(MapSource mapSource) {
		if (!(mapSource instanceof HttpMapSource)) {
			return;
		}
		if (testedMapSources.contains(mapSource.getName())) {
			return;
		}
		EastNorthCoordinate coordinate = Cities.getTestCoordinate(mapSource, C_DEFAULT);
		addTest(new MapSourceTestCase((HttpMapSource) mapSource, coordinate));
		testedMapSources.add(mapSource.getName());
	}

}
