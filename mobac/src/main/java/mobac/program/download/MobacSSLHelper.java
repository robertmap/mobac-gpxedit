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
package mobac.program.download;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class MobacSSLHelper {

	public static SSLSocketFactory createSSLSocketFactory(Set<String> additionalTrustedPublicKeys) {
		MobacTrustManager trustManager = new MobacTrustManager(additionalTrustedPublicKeys);
		SSLContext sslcontext;
		try {
			sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(new KeyManager[0], new TrustManager[]{trustManager}, null);
			return sslcontext.getSocketFactory();
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}
}
