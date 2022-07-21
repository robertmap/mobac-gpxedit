package mobac.mapsources;

import mobac.program.interfaces.MapSource;

import java.util.Vector;

public class MapEvaluatorMapSourcesManager extends MapSourcesManager {

	private final Vector<MapSource> mapSources = new Vector<>();

	public static void initialitze() {
		MapSourcesManager.INSTANCE = new MapEvaluatorMapSourcesManager();
	}

	private MapEvaluatorMapSourcesManager() {
	}

	@Override
	public void addMapSource(MapSource mapSource) {
	}

	@Override
	public Vector<MapSource> getAllAvailableMapSources() {
		return mapSources;
	}

	@Override
	public Vector<MapSource> getAllLayerMapSources() {
		return mapSources;
	}

	@Override
	public Vector<MapSource> getAllMapSources() {
		return mapSources;
	}

	@Override
	public MapSource getDefaultMapSource() {
		return mapSources.get(0);
	}

	@Override
	public Vector<MapSource> getDisabledMapSources() {
		return new Vector<MapSource>();
	}

	@Override
	public Vector<MapSource> getEnabledOrderedMapSources() {
		return mapSources;
	}

	@Override
	public MapSource getSourceByName(String name) {
		throw new RuntimeException("Not implemented");
	}

}
