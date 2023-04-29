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
package mobac.mapsources;

import mobac.program.download.MobacSSLHelper;
import mobac.program.interfaces.HttpMapSource;
import mobac.program.interfaces.MapSourceListener;
import mobac.program.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

public abstract class AbstractHttpMapSourceBase implements HttpMapSource {
	protected static final SSLSocketFactory SSL_SOCKET_FACTORY = MobacSSLHelper.createSSLSocketFactory(null);

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public void prepareConnection(HttpURLConnection connection) throws ProtocolException {
		if (connection instanceof HttpsURLConnection) {
			((HttpsURLConnection) connection).setSSLSocketFactory(getSslSocketFactory());
		}

		Settings settings = Settings.getInstance();

		connection.setRequestMethod("GET");

		connection.setConnectTimeout(1000 * settings.httpConnectionTimeout);
		connection.setReadTimeout(1000 * settings.httpReadTimeout);
		if (connection.getRequestProperty("User-agent") == null) {
			connection.setRequestProperty("User-agent", settings.getUserAgent());
		}
		connection.setRequestProperty("Accept", settings.getHttpAccept());
		if (Thread.currentThread() instanceof MapSourceListener) {
			((MapSourceListener) Thread.currentThread()).tileDownloadStarted(connection.getURL().toString());
		}
	}

	protected SSLSocketFactory getSslSocketFactory() {
		return SSL_SOCKET_FACTORY;
	}

}
