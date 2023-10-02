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

import bsh.EvalError;
import bsh.Interpreter;
import jakarta.xml.bind.UnmarshalException;
import mobac.exceptions.MapSourceInitializationException;
import mobac.exceptions.TileException;
import mobac.gui.mapview.PreviewMap;
import mobac.mapsources.AbstractHttpMapSource;
import mobac.mapsources.mapspace.MapSpaceFactory;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.download.MobacSSLHelper;
import mobac.program.interfaces.MapSpace;
import mobac.program.interfaces.ReloadableMapSource;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.TileImageType;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class BeanShellHttpMapSource extends AbstractHttpMapSource
		implements
			ReloadableMapSource<BeanShellHttpMapSource> {

	private static final String AH_ERROR = "Sourced file: inline evaluation of: "
			+ "``addHeaders(conn);'' : Command not found: addHeaders( sun.net.www.protocol.http.HttpURLConnection )";

	private static final Logger LOG = LoggerFactory.getLogger(BeanShellHttpMapSource.class);

	private static int NUM = 0;

	private final String bshMapName;

	private String code;

	private Interpreter interpreter;

	private boolean hasAddHeadersMethod;

	private Color backgroundColor = Color.BLACK;

	private boolean ignoreError = false;

	private String displayName = null;

	private SSLSocketFactory sslSocketFactory = AbstractHttpMapSource.SSL_SOCKET_FACTORY;

	public BeanShellHttpMapSource(String code, String bshMapName) throws EvalError {
		super("", 0, 0, TileImageType.PNG, TileUpdate.None);
		this.bshMapName = bshMapName;
		this.code = code;
		name = "BeanShell map source " + NUM++;
		prepareInterpreter(code);
	}

	public static BeanShellHttpMapSource load(File f) throws EvalError, IOException {
		return new BeanShellHttpMapSource(FileUtils.readFileToString(f, StandardCharsets.UTF_8), f.getName());
	}

	protected void prepareInterpreter(String code) throws EvalError {
		interpreter = new Interpreter();
		interpreter.set("LOG", LOG);

		interpreter.eval("import mobac.program.interfaces.HttpMapSource.TileUpdate;");
		interpreter.eval("import java.net.HttpURLConnection;");
		interpreter.eval("import mobac.utilities.beanshell.*;");
		interpreter.eval(code);
		Object o = interpreter.get("name");
		if (o != null) {
			name = (String) o;
		}
		o = interpreter.get("displayName");
		if (o != null) {
			displayName = (String) o;
		}

		o = interpreter.get("tileSize");
		if (o != null) {
			int tileSize = ((Integer) o).intValue();
			mapSpace = MapSpaceFactory.getInstance(tileSize, true);
		} else {
			mapSpace = MercatorPower2MapSpace.INSTANCE_256;
		}

		o = interpreter.get("minZoom");
		if (o != null) {
			minZoom = ((Integer) o).intValue();
		} else {
			minZoom = 0;
		}

		o = interpreter.get("maxZoom");
		if (o != null) {
			maxZoom = ((Integer) o).intValue();
		} else {
			maxZoom = PreviewMap.MAX_ZOOM;
		}

		o = interpreter.get("tileType");
		if (o != null) {
			tileType = TileImageType.getTileImageType((String) o);
		} else {
			throw new EvalError("tileType definition missing", null, null);
		}

		o = interpreter.get("tileUpdate");
		if (o != null) {
			tileUpdate = (TileUpdate) o;
		}

		o = interpreter.get("ignoreError");
		if (o != null) {
			if (o instanceof String) {
				ignoreError = Boolean.parseBoolean((String) o);
			} else if (o instanceof Boolean) {
				ignoreError = ((Boolean) o).booleanValue();
			} else {
				throw new EvalError("Invalid type for \"ignoreError\": " + o.getClass(), null, null);
			}
		}

		o = interpreter.get("backgroundColor");
		if (o != null) {
			try {
				backgroundColor = ColorAdapter.parseColor((String) o);
			} catch (UnmarshalException e) {
				throw new EvalError(e.getMessage(), null, null);
			}
		}

		o = interpreter.get("trustedPublicKeyHash");
		if (o != null) {
			TreeSet<String> publicKeyHashes = new TreeSet<>();
			publicKeyHashes.add(((String) o).toLowerCase());
			this.sslSocketFactory = MobacSSLHelper.createSSLSocketFactory(publicKeyHashes);
		}

		List<String> methodNames = Arrays.asList(interpreter.getNameSpace().getMethodNames());
		hasAddHeadersMethod = methodNames.contains("addHeaders");
		if (!hasAddHeadersMethod) {
			log.warn("Beanshell \"" + bshMapName + "\" (" + name
					+ ") has no addHeaders method - addHeaders will not be called!");
		}
	}

	@Override
	public synchronized HttpURLConnection getTileUrlConnection(int zoom, int tilex, int tiley) throws IOException {
		HttpURLConnection conn = null;
		try {
			String url = getTileUrl(zoom, tilex, tiley);
			conn = (HttpURLConnection) new URL(url).openConnection();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			log.error("", e);
			throw new IOException(e);
		}
		if (hasAddHeadersMethod) {
			try {
				interpreter.set("conn", conn);
				interpreter.eval("addHeaders(conn);");
			} catch (EvalError e) {
				String msg = e.getMessage();
				if (!AH_ERROR.equals(msg)) {
					log.error(e.getClass() + ": " + e.getMessage(), e);
					throw new IOException(e);
				}
			}
		}
		return conn;
	}

	@Override
	protected SSLSocketFactory getSslSocketFactory() {
		return this.sslSocketFactory;
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, TileException, InterruptedException {
		try {
			return super.getTileImage(zoom, x, y, loadMethod);
		} catch (Exception e) {
			if (ignoreError) {
				log.error("Ignored error: " + e);
				return null;
			}
			throw e;
		}
	}

	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, TileException, InterruptedException {
		try {
			return super.getTileData(zoom, x, y, loadMethod);
		} catch (Exception e) {
			if (ignoreError) {
				log.error("Ignored error: " + e);
				return null;
			}
			throw e;
		}
	}

	public boolean testCode() throws IOException {
		return (getTileUrlConnection(minZoom, 0, 0) != null);
	}

	public String getTileUrl(int zoom, int tilex, int tiley) {
		try {
			return (String) interpreter.eval(String.format("getTileUrl(%d,%d,%d);", zoom, tilex, tiley));
		} catch (EvalError e) {
			log.error(e.getClass() + ": " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void applyChangesFrom(BeanShellHttpMapSource reloadedMapSource) throws MapSourceInitializationException {
		if (!name.equals(reloadedMapSource.getName())) {
			throw new MapSourceInitializationException("The map name has changed");
		}
		this.code = reloadedMapSource.code;
		try {
			prepareInterpreter(code);
		} catch (EvalError e) {
			throw new MapSourceInitializationException(e);
		}
	}

	@Override
	public MapSpace getMapSpace() {
		return mapSpace;
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
		if (displayName != null) {
			return displayName;
		}
		return name;
	}

	@Override
	public TileUpdate getTileUpdate() {
		return tileUpdate;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

}
