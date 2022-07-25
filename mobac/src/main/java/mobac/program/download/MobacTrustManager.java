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

import mobac.exceptions.NotImplementedException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * Custom delegating {@link TrustManager} that allows to specify public key
 * hashes of leaf certificates to directly trust the certificate.
 * <p>
 * The following OpenSSL command can be used to generate the public key SHA-256
 * hash:
 *
 * <pre>
 * openssl s_client -host maps.example.org -port 443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256
 * </pre>
 */
public class MobacTrustManager implements X509TrustManager {

	private static final Logger log = LoggerFactory.getLogger(MobacTrustManager.class);

	private final X509TrustManager defaultTrustManager;

	private final Set<String> additionalTrustedPublicKeys;

	public MobacTrustManager() {
		this(null);
	}

	public MobacTrustManager(Set<String> additionalTrustedPublicKeys) {
		super();
		this.additionalTrustedPublicKeys = additionalTrustedPublicKeys;
		TrustManagerFactory tmf;
		try {
			tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore) null);
		} catch (KeyStoreException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		X509TrustManager defaultTm = null;
		for (TrustManager tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				defaultTm = (X509TrustManager) tm;
				break;
			}
		}
		if (defaultTm == null) {
			throw new RuntimeException("Failed to get default Trustmanager");
		}
		defaultTrustManager = defaultTm;
	}

	private static String getPublicKeySha256Hash(X509Certificate cert) {
		byte[] pubKeyData = cert.getPublicKey().getEncoded();
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(pubKeyData);
			return Hex.encodeHexString(digest).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getServerPublicKeyHash(String serverUrl)
			throws IOException, KeyManagementException, NoSuchAlgorithmException {
		URL url = new URL(serverUrl);

		SSLContext sslcontext = SSLContext.getInstance("TLS");
		GetPublicKeyHashTrustManager trustManager = new GetPublicKeyHashTrustManager();
		sslcontext.init(new KeyManager[0], new TrustManager[]{trustManager}, null);

		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setSSLSocketFactory(sslcontext.getSocketFactory());
		try {
			conn.connect();
			throw new RuntimeException("Unreachable code reached");
		} catch (SSLHandshakeException e) {
			// It is expected that we end up here
			if (trustManager.serverPublicKeyHash == null) {
				throw new RuntimeException("Unable to get server certificate: " + e.getMessage(), e);
			}
		}
		return trustManager.serverPublicKeyHash;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		throw new NotImplementedException();
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			defaultTrustManager.checkServerTrusted(chain, authType);
		} catch (CertificateException e) {
			X509Certificate cert = chain[0]; // get the leaf certificate
			log.error("SSL error: " + e.getMessage());
			synchronized (this) {
				String pubKeySha256Hash = getPublicKeySha256Hash(cert);
				if (isCertificateTrusted(pubKeySha256Hash)) {
					return; // certificate is trusted
				}
				// TODO: Add GUI for manually adding this certificate as trusted.
				String message = "Untrusted certificate encountered: publicKeyHash=\"" + pubKeySha256Hash
						+ "\"; certificate issued for " + cert.getSubjectDN();
				throw new CertificateException(message);
			}
		}
	}

	private boolean isCertificateTrusted(String pubKeySha256Hash) {
		if (additionalTrustedPublicKeys != null) {
			return additionalTrustedPublicKeys.contains(pubKeySha256Hash);
		}
		return false;
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return defaultTrustManager.getAcceptedIssuers();
	}

	private static class GetPublicKeyHashTrustManager implements X509TrustManager {
		public String serverPublicKeyHash = null;

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			X509Certificate cert = chain[0];
			serverPublicKeyHash = getPublicKeySha256Hash(cert);
			throw new CertificateException();
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
