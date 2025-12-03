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
package mobac.program.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessorOrder;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import mobac.gui.actions.GpxLoad;
import mobac.gui.panels.JCoordinatesPanel;
import mobac.program.DirectoryManager;
import mobac.program.ProgramInfo;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;
import mobac.utilities.stream.ThrottledInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static mobac.gui.MainGUI.LEFT_PANEL_MIN_SIZE;

@SuppressWarnings("CanBeFinal")
@XmlRootElement
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class Settings {

	public static final File FILE = new File(DirectoryManager.userSettingsDir, "settings.xml");
	private static final Logger log = LoggerFactory.getLogger(Settings.class);
	private static final String SYSTEM_PROXY_HOST = System.getProperty("http.proxyHost");
	private static final String SYSTEM_PROXY_PORT = System.getProperty("http.proxyPort");

	private static long SETTINGS_LAST_MODIFIED = 0;
	private static Settings instance = new Settings();
	public final Directories directories = new Directories();
	public final AtlasFormatSpecificSettings atlasFormatSpecificSettings = new AtlasFormatSpecificSettings();
	public final MainWindowSettings mainWindow = new MainWindowSettings();
	@XmlElement(name = "MapSourcesUpdate")
	public final MapSourcesUpdate mapSourcesUpdate = new MapSourcesUpdate();
	public final SettingsPaperAtlas paperAtlas = new SettingsPaperAtlas();
	public final SettingsWgsGrid wgsGrid = new SettingsWgsGrid();
	public int maxMapSize = 65536;
	public int mapOverlapTiles = 0;
	public boolean tileStoreEnabled = true;
	/**
	 * Mapview related settings
	 */
	public int mapviewZoom = 3;
	public int mapviewGridZoom = -1;
	public EastNorthCoordinate mapviewCenterCoordinate = new EastNorthCoordinate(50, 9);
	public Point mapviewSelectionMax = null;
	public Point mapviewSelectionMin = null;
	@XmlElementWrapper(name = "selectedZoomLevels")
	@XmlElement(name = "zoomLevel")
	public List<Integer> selectedZoomLevels = null;
	private static final String ACCEPT = "text/html,image/png,image/jpeg,image/gif,image/webp,*/*; q=0.1";
	@XmlElement(nillable = false)
	public String mapviewMapSource;
	public int downloadThreadCount = 2;
	public int downloadRetryCount = 1;
	public CoordinateStringFormat coordinateNumberFormat = CoordinateStringFormat.DEG_LOCAL;
	@XmlElementWrapper(name = "placeBookmarks")
	@XmlElement(name = "bookmark")
	public List<Bookmark> placeBookmarks = new ArrayList<>();
	/**
	 * Connection timeout in seconds (default 10 seconds)
	 */
	public int httpConnectionTimeout = 10;
	/**
	 * Read timeout in seconds (default 10 seconds)
	 */
	public int httpReadTimeout = 10;
	/**
	 * Maximum expiration (in milliseconds) acceptable. If a server sets an
	 * expiration time larger than this value it is truncated to this value on next
	 * download.
	 */
	public long tileMaxExpirationTime = TimeUnit.DAYS.toMillis(365);
	/**
	 * Minimum expiration (in milliseconds) acceptable. If a server sets an
	 * expiration time smaller than this value it is truncated to this value on next
	 * download.
	 */
	public long tileMinExpirationTime = TimeUnit.DAYS.toMillis(5);
	/**
	 * Expiration time (in milliseconds) of a tile if the server does not provide an
	 * expiration time
	 */
	public long tileDefaultExpirationTime = TimeUnit.DAYS.toMillis(28);
	public String googleLanguage = "en";
	public String osmHikingTicket = "";
	/**
	 * Development mode enabled/disabled
	 * <p>
	 * In development mode one additional map source is available for using MOBAC
	 * Debug TileServer
	 * </p>
	 */
	@XmlElement
	public boolean devMode = false;
	/**
	 * Saves the last used directory of the GPX file chooser dialog. Used in
	 * {@link GpxLoad}.
	 */
	public String gpxFileChooserDir = "";
	@XmlElementWrapper(name = "mapSourcesDisabled")
	@XmlElement(name = "mapSource")
	public Vector<String> mapSourcesDisabled = new Vector<>();
	@XmlElementWrapper(name = "mapSourcesEnabled")
	@XmlElement(name = "mapSource")
	public Vector<String> mapSourcesEnabled = new Vector<>();
	public transient UnitSystem unitSystem = UnitSystem.Metric;
	public boolean ignoreDlErrors = false;
	public String elementName;
	public String localeLanguage;
	@XmlElement(defaultValue = "")
	private String version;
	public String localeCountry;
	private String userAgent;
	private boolean customTileProcessing = false;
	private Dimension tileSize = new Dimension(256, 256);
	private TileImageFormat tileImageFormat = TileImageFormat.PNG;
	/**
	 * Network settings
	 */
	private ProxyType proxyType = ProxyType.CUSTOM;
	private String customProxyHost = "";
	private String customProxyPort = "";
	private String customProxyUserName = "";
	private String customProxyPassword = "";
	private long bandwidthLimit = 0;

	private Settings() {
		elementName = "Layer";// no need i18n for it
		Dimension dScreen = Toolkit.getDefaultToolkit().getScreenSize();
		mainWindow.size.width = (int) (0.9f * dScreen.width);
		mainWindow.size.height = (int) (0.9f * dScreen.height);
		mainWindow.collapsedPanels.add(JCoordinatesPanel.NAME);
		mainWindow.collapsedPanels.add("Gpx");

		Locale defaultLocale = Locale.getDefault();
		boolean localeFound = false;
		for (SupportedLocale l : SupportedLocale.values()) {
			if (defaultLocale.equals(l.getLocale())) {
				localeLanguage = l.getLocale().getLanguage();
				localeCountry = l.getLocale().getCountry();
				localeFound = true;
				break;
			}
		}
		if (!localeFound) {
			localeLanguage = "en";
			localeCountry = "";
		}
	}

	public static Settings getInstance() {
		return instance;
	}

	private String httpAccept;

	public static boolean checkSettingsFileModified() {
		if (SETTINGS_LAST_MODIFIED == 0) {
			return false;
		}
		// Check if the settings.xml has been modified
		// since it has been loaded
		long lastModified = FILE.lastModified();
		return (SETTINGS_LAST_MODIFIED != lastModified);
	}

	public static void save() throws JAXBException {
		getInstance().version = ProgramInfo.getVersion();
		JAXBContext context = JAXBContext.newInstance(Settings.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// First we write to a buffer and if that works be write the buffer
		// to disk. Direct writing to file may result in an defect xml file
		// in case of an error
		StringWriter sw = new StringWriter(4096);
		m.marshal(getInstance(), sw);
		String settingsString = sw.toString();

		try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
			writer.write(settingsString);
		} catch (IOException e) {
			throw new JAXBException(e);
		}
		SETTINGS_LAST_MODIFIED = FILE.lastModified();
	}

	public static void loadOrQuit() {
		try {
			load();
		} catch (JAXBException e) {
			log.error(e.getMessage(), e);
			JOptionPane.showMessageDialog(null,
					I18nUtils.localizedStringForKey(I18nUtils.localizedStringForKey("msg_settings_file_can_not_parse")),
					I18nUtils.localizedStringForKey("Error"), JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}

	public String getUserAgent() {
		if (userAgent != null) {
			return userAgent;
		} else {
			return ProgramInfo.getUserAgent();
		}
	}

	public void setUserAgent(String userAgent) {
		if (userAgent != null) {
			userAgent = userAgent.trim();
			if (userAgent.isEmpty()) {
				userAgent = null;
			}
		}
		this.userAgent = userAgent;
	}

	public static void load() throws JAXBException {
		try {
			JAXBContext context = JAXBContext.newInstance(Settings.class);
			Unmarshaller um = context.createUnmarshaller();
			um.setEventHandler((event) -> {
				log.warn("Problem on loading settings.xml: " + event.getMessage());
				return true;
			});
			instance = (Settings) um.unmarshal(FILE);
			instance.wgsGrid.checkValues();
			instance.paperAtlas.checkValues();
			SETTINGS_LAST_MODIFIED = FILE.lastModified();

			// Settings 重新加载之后，必须更新语言资源
			I18nUtils.updateLocalizedStringFromSettings();

		} finally {
			Settings s = getInstance();
			s.applyProxySettings();
		}
	}

	public String getHttpAccept() {
		if (httpAccept != null) {
			return httpAccept;
		}
		return ACCEPT;
	}

	public void setHttpAccept(String httpAccept) {
		if (httpAccept != null) {
			httpAccept = httpAccept.trim();
			if (httpAccept.isEmpty()) {
				httpAccept = null;
			}
		}
		this.httpAccept = httpAccept;
	}

	public boolean isCustomTileSize() {
		return customTileProcessing;
	}

	public void setCustomTileSize(boolean customTileSize) {
		this.customTileProcessing = customTileSize;
	}

	public Dimension getTileSize() {
		return tileSize;
	}

	public void setTileSize(Dimension tileSize) {
		this.tileSize = tileSize;
	}

	public TileImageFormat getTileImageFormat() {
		return tileImageFormat;
	}

	public void setTileImageFormat(TileImageFormat tileImageFormat) {
		this.tileImageFormat = tileImageFormat;
	}

	public ProxyType getProxyType() {
		return proxyType;
	}

	public void setProxyType(ProxyType proxyType) {
		this.proxyType = proxyType;
	}

	public String getCustomProxyHost() {
		return customProxyHost;
	}

	public void setCustomProxyHost(String proxyHost) {
		this.customProxyHost = proxyHost;
	}

	public String getCustomProxyPort() {
		return customProxyPort;
	}

	public void setCustomProxyPort(String proxyPort) {
		this.customProxyPort = proxyPort;
	}

	public String getCustomProxyUserName() {
		return customProxyUserName;
	}

	public void setCustomProxyUserName(String customProxyUserName) {
		this.customProxyUserName = customProxyUserName;
	}

	public String getCustomProxyPassword() {
		return customProxyPassword;
	}

	public void setCustomProxyPassword(String customProxyPassword) {
		this.customProxyPassword = customProxyPassword;
	}

	public void applyProxySettings() {
		boolean useSystemProxies = false;
		String newProxyHost = null;
		String newProxyPort = null;
		Authenticator newAuthenticator = null;
		switch (proxyType) {
			case SYSTEM :
				log.info("Applying proxy configuration: system settings");
				useSystemProxies = true;
				break;
			case APP_SETTINGS :
				newProxyHost = SYSTEM_PROXY_HOST;
				newProxyPort = SYSTEM_PROXY_PORT;
				log.info("Applying proxy configuration: host=" + newProxyHost + " port=" + newProxyPort);
				break;
			case CUSTOM :
				newProxyHost = customProxyHost;
				newProxyPort = customProxyPort;
				log.info("Applying proxy configuration: host=" + newProxyHost + " port=" + newProxyPort);
				break;
			case CUSTOM_W_AUTH :
				newProxyHost = customProxyHost;
				newProxyPort = customProxyPort;
				newAuthenticator = new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(customProxyUserName, customProxyPassword.toCharArray());
					}
				};
				log.info("Applying proxy configuration: host=" + newProxyHost + " port=" + newProxyPort + " user="
						+ customProxyUserName);
				break;
		}
		Utilities.setHttpProxyHost(newProxyHost);
		Utilities.setHttpProxyPort(newProxyPort);
		Authenticator.setDefault(newAuthenticator);
		System.setProperty("java.net.useSystemProxies", Boolean.toString(useSystemProxies));
	}

	public long getBandwidthLimit() {
		return bandwidthLimit;
	}

	public void setBandwidthLimit(long bandwidthLimit) {
		this.bandwidthLimit = bandwidthLimit;
		ThrottledInputStream.setBandwidth(bandwidthLimit);
	}

	public UnitSystem getUnitSystem() {
		return unitSystem;
	}

	@XmlElement
	public void setUnitSystem(UnitSystem unitSystem) {
		if (unitSystem == null) {
			unitSystem = UnitSystem.Metric;
		}
		this.unitSystem = unitSystem;
	}

	@XmlTransient
	public File getMapSourcesDirectory() {
		String mapSourcesDirCfg = directories.mapSourcesDirectory;
		File mapSourcesDir;
		if (mapSourcesDirCfg == null || mapSourcesDirCfg.trim().isEmpty()) {
			mapSourcesDir = DirectoryManager.mapSourcesDir;
		} else {
			mapSourcesDir = new File(mapSourcesDirCfg);
		}
		return mapSourcesDir;
	}

	@XmlTransient
	public File getAtlasOutputDirectory() {
		if (directories.atlasOutputDirectory != null) {
			return new File(directories.atlasOutputDirectory);
		}
		return new File(DirectoryManager.currentDir, "atlases");
	}

	/**
	 * @param dir
	 *            <code>null</code> or empty string resets to default directory
	 *            otherwise set the new atlas output directory.
	 */
	public void setAtlasOutputDirectory(String dir) {
		if (dir != null && dir.trim().isEmpty()) {
			dir = null;
		}
		directories.atlasOutputDirectory = dir;
	}

	public String getAtlasOutputDirectoryString() {
		if (directories.atlasOutputDirectory == null) {
			return "";
		}
		return directories.atlasOutputDirectory;
	}

	public String getVersion() {
		return version;
	}

	public static class Directories {
		@XmlElement
		public String tileStoreDirectory;
		@XmlElement
		private String atlasOutputDirectory = null;
		@XmlElement
		private String mapSourcesDirectory;
	}

	public static class AtlasFormatSpecificSettings {

		@XmlElement
		public Integer garminCustomMaxMapCount = 100;
	}

	public static class MainWindowSettings {
		public Dimension size = new Dimension();
		public Point position = new Point(-1, -1);
		public boolean maximized = true;

		public boolean rightPanelVisible = true;
		public boolean leftPanelVisible = true;

		/**
		 * Width of the left panel. Default value it taken from
		 * MainGUI.LEFT_PANEL_MIN_SIZE
		 */
		public int leftPanelWidth = LEFT_PANEL_MIN_SIZE;

		@XmlElementWrapper(name = "collapsedPanels")
		@XmlElement(name = "collapsedPanel")
		public Vector<String> collapsedPanels = new Vector<>();
	}

	public static class MapSourcesUpdate {
		public String etag;

		public Date lastUpdate;
	}

}
