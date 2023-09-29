package mobac.mapsources.loader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEventLocator;
import mobac.exceptions.MapSourceCreateException;
import mobac.exceptions.MapSourceInitializationException;
import mobac.mapsources.MapSourcesManager;
import mobac.mapsources.custom.CustomLocalAqmMapSource;
import mobac.mapsources.custom.CustomLocalTileFilesMapSource;
import mobac.mapsources.custom.CustomLocalTileSQliteMapSource;
import mobac.mapsources.custom.CustomLocalTileZipMapSource;
import mobac.mapsources.custom.CustomMapSource;
import mobac.mapsources.custom.CustomMapsforge;
import mobac.mapsources.custom.CustomMultiLayerMapSource;
import mobac.mapsources.custom.CustomWmsMapSource;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.ReloadableMapSource;
import mobac.program.interfaces.WrappedMapSource;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.MapSourceLoaderInfo.LoaderType;
import mobac.utilities.Utilities;
import mobac.utilities.file.DirOrFileExtFilter;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CustomMapSourceLoader {

	private static final String MOBAC_IGNORE_TAG = "mobac-ignore:";
	private final Logger log = LoggerFactory.getLogger(MapPackManager.class);
	private final MapSourcesManager mapSourcesManager;
	private final File mapSourcesDir;

	private final DocumentBuilderFactory dbFactory;
	private final DocumentBuilder dBuilder;
	private final JAXBContext context;

	public CustomMapSourceLoader(MapSourcesManager mapSourceManager, File mapSourcesDir) {
		this.mapSourcesManager = mapSourceManager;
		this.mapSourcesDir = mapSourcesDir;
		dbFactory = DocumentBuilderFactory.newInstance();
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		try {
			Class<?>[] customMapClasses = new Class[]{ //
					//
					CustomMapSource.class, //
					CustomWmsMapSource.class, //
					CustomMultiLayerMapSource.class, //
					// CustomCombinedMapSource.class, //
					CustomMapsforge.class, //
					CustomLocalTileFilesMapSource.class, //
					CustomLocalTileZipMapSource.class, //
					CustomLocalTileSQliteMapSource.class, //

					CustomLocalAqmMapSource.class};
			context = JAXBContext.newInstance(customMapClasses);
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to create JAXB context for custom map sources", e);
		}
	}

	public List<File> getMapSourceFiles() {
		List<File> customMapSourceFiles = Utilities.traverseFolder(mapSourcesDir, new DirOrFileExtFilter(".xml"));
		/*
		 * It is important to sort the files to be loaded, otherwise the order would be
		 * random which makes it difficult to reference custom map sources in a
		 * multi-layer map source if the referenced map source has not been loaded
		 * before.
		 *
		 * See https://sourceforge.net/p/mobac/bugs/294/
		 */
		Collections.sort(customMapSourceFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
		return customMapSourceFiles;
	}

	public void loadCustomMapSources() {

		for (File f : getMapSourceFiles()) {
			try {
				MapSource customMapSource = loadCustomMapSource(f);
				if (customMapSource == null) {
					log.info("Ignoring xml file \"{}\" - not a custom MOBAC XML map file", f.getName());
					continue; // an element to be ignored
				}
				if (!(customMapSource instanceof FileBasedMapSource) && customMapSource.getTileImageType() == null) {
					log.warn("A problem occurred while loading \"{}\": tileType is null - "
							+ "some atlas formats will produce an error!", f.getName());
				}
				mapSourcesManager.addMapSource(customMapSource);
			} catch (Exception e) {
				log.error("failed to load custom map source \"{}\": {}", f.getName(), e.getMessage(), e);
			}
		}
	}

	public MapSource loadCustomMapSource(File mapSourceFile)
			throws MapSourceCreateException, JAXBException, IOException {

		List<String> elementFilter = new LinkedList<>();
		try {
			Document doc = dBuilder.parse(mapSourceFile);
			Element rootElem = doc.getDocumentElement();
			if ("rendertheme".equals(rootElem.getTagName())) {
				// This is a Mapsforge render theme xml file, not a MOBAC custom map file
				return null;
			}

			// Check of the MOBAC_IGNORE_TAG in all comments on root level
			NodeList children = doc.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node n = children.item(i);
				if (n instanceof Comment) {
					String comment = n.getNodeValue().trim();
					if (comment.startsWith(MOBAC_IGNORE_TAG)) {
						comment = comment.substring(MOBAC_IGNORE_TAG.length()).trim();
						Collections.addAll(elementFilter, comment.split("[,;\\s]+"));
					}
				}

			}
		} catch (Exception e) {
			log.error("Failed to load custom map source file \"" + mapSourceFile + "\": " + e);
		}
		try (InputStream in = new FileInputStream(mapSourceFile)) {
			return internalLoadMapSource(in, mapSourceFile, elementFilter);
		}
	}

	public MapSource loadCustomMapSource(InputStream in) throws MapSourceCreateException, JAXBException {
		return internalLoadMapSource(in, null, null);
	}

	/**
	 * Load custom map source from XML document DOM
	 *
	 * @param in
	 * @param loaderInfoFile
	 * @param elementFilter
	 * @return
	 * @throws MapSourceCreateException
	 * @throws JAXBException
	 */
	protected MapSource internalLoadMapSource(InputStream in, final File loaderInfoFile,
			Collection<String> elementFilter) throws MapSourceCreateException, JAXBException {
		Unmarshaller unmarshaller = context.createUnmarshaller();

		unmarshaller.setEventHandler(event -> {
			ValidationEventLocator loc = event.getLocator();
			String file = "";
			String dir = null;
			if (loaderInfoFile != null) {
				file = loaderInfoFile.getName();
				dir = loaderInfoFile.getParent();
			}
			int lastSlash = file.lastIndexOf('/');
			if (lastSlash > 0) {
				file = file.substring(lastSlash + 1);
			}

			String errorMsg = event.getMessage();
			if (errorMsg == null) {
				Throwable t = event.getLinkedException();
				while (t != null && errorMsg == null) {
					errorMsg = t.getMessage();
					t = t.getCause();
				}
			}

			String message = "<html><h3>Failed to load a custom map</h3><p><i>" + errorMsg + "</i></p><br><p>";
			if (dir != null) {
				message += "directory: \"<b>" + StringEscapeUtils.escapeHtml4(dir) + "</b>\"<br>";
			}
			message += "file: \"<b>" + StringEscapeUtils.escapeHtml4(file) + "</b>\"<br>" + "line/column: <i>"
					+ loc.getLineNumber() + "/" + loc.getColumnNumber() + "</i></p>";

			JOptionPane.showMessageDialog(null, message, "Error: custom map loading failed", JOptionPane.ERROR_MESSAGE);
			log.error(event.toString());
			return false;
		});
		Object o;
		if (elementFilter != null && !elementFilter.isEmpty()) {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			try {
				XMLStreamReader streamReader = factory.createXMLStreamReader(in);
				XMLStreamReader filteredStreamReader = factory.createFilteredReader(streamReader,
						new XmlFilter(elementFilter));
				o = unmarshaller.unmarshal(filteredStreamReader);
			} catch (XMLStreamException e) {
				throw new JAXBException(e);
			}
		} else {
			o = unmarshaller.unmarshal(in);
		}
		MapSource customMapSource;
		if (o instanceof WrappedMapSource) {
			customMapSource = ((WrappedMapSource) o).getMapSource();
		} else {
			customMapSource = (MapSource) o;
		}
		customMapSource.setLoaderInfo(new MapSourceLoaderInfo(LoaderType.XML, loaderInfoFile));
		if (loaderInfoFile != null) {
			log.trace("Custom map source loaded: {} from file \"{}\"", customMapSource, loaderInfoFile.getName());
		} else {
			log.trace("Custom map source loaded: {}", customMapSource);
		}
		return customMapSource;
	}

	@SuppressWarnings("unchecked")
	public boolean reloadCustomMapSource(MapSource mapSource) throws MapSourceCreateException, JAXBException,
			IOException, SAXException, MapSourceInitializationException {
		MapSourceLoaderInfo loaderInfo = mapSource.getLoaderInfo();
		if ((loaderInfo == null) || (loaderInfo.getLoaderType() != LoaderType.XML)) {
			return false;
		}
		if (!(mapSource instanceof ReloadableMapSource)) {
			return false;
		}
		if (!loaderInfo.checkSourcesfileChanged()) {
			return false; // file has not changed
		}

		MapSource updatedMapSource = loadCustomMapSource(loaderInfo.getSourceFile());

		if (!mapSource.getClass().getName().equals(updatedMapSource.getClass().getName())) {
			throw new RuntimeException(
					"Reloading failed: The map source type has changed in file \"" + loaderInfo.getSourceFile() + "\"");
		}

		if (!mapSource.getName().equals(updatedMapSource.getName())) {
			throw new RuntimeException(
					"Reloading failed: The map source name has changed in file \"" + loaderInfo.getSourceFile() + "\"");
		}

		((ReloadableMapSource<MapSource>) mapSource).applyChangesFrom(updatedMapSource);
		log.debug("Map source reloaded: \"{}\"", loaderInfo.getSourceFile());

		return true;
	}

	private static class XmlFilter implements StreamFilter {

		private final Set<String> filterOut;
		private int level = 0;

		private XmlFilter(Collection<String> filterOut) {
			this.filterOut = new HashSet<>(filterOut);
		}

		@Override
		public boolean accept(XMLStreamReader reader) {
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (level == 1 && filterOut.contains(reader.getName().getLocalPart())) {
					return false;
				}
				level++;
			} else if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
				level--;
			}
			return true;
		}
	}
}
