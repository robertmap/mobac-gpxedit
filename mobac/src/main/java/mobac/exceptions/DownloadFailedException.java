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
package mobac.exceptions;

import mobac.utilities.HtmlStrip;
import mobac.utilities.imageio.ImageFormatDetector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadFailedException extends IOException {

	private static final Logger log = LoggerFactory.getLogger(DownloadFailedException.class);

	private static final Pattern CHARSET = Pattern.compile("charset=([a-zA-Z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);

	private final HttpURLConnection connection;
	private final byte[] responseData;
	private final boolean typeImage;

	public DownloadFailedException(HttpURLConnection connection, byte[] responseData) throws IOException {
		this(connection, responseData, null);
	}

	public DownloadFailedException(HttpURLConnection connection, byte[] responseData, Throwable cause)
			throws IOException {
		super("HTTP error: " + connection.getResponseCode(), cause);
		this.connection = connection;
		this.responseData = responseData;
		this.typeImage = ImageFormatDetector.getImageType(responseData) != null;
	}

	public int getHttpResponseCode() {
		try {
			return connection.getResponseCode();
		} catch (IOException e) {
			return -1;
		}
	}

	public boolean isTypeImage() {
		return typeImage;
	}

	public byte[] getResponseData() {
		return responseData;
	}

	public String generateResponseErrorText() {
		try {
			String contentType = connection.getContentType();
			String payload = "";

			if (responseData != null
					&& StringUtils.startsWithAny(contentType, "text/", "application/xml", "application/json")) {
				Matcher charsetMatcher = CHARSET.matcher(contentType);
				Charset contentCharset = StandardCharsets.UTF_8;
				try {
					if (charsetMatcher.find()) {
						contentCharset = Charset.forName(charsetMatcher.group(1));
					} else if (contentType.contains("html")) {
						String responseString = new String(responseData, StandardCharsets.UTF_8);
						charsetMatcher = CHARSET.matcher(responseString);
						if (charsetMatcher.find()) {
							contentCharset = Charset.forName(charsetMatcher.group(1));
						}
					}
				} catch (IllegalArgumentException e) {
					// ignore and continue with default charset
				}

				int contentTypeEnd = contentType.indexOf(';');
				if (contentTypeEnd > 0) {
					contentType = contentType.substring(0, contentTypeEnd).trim();
				}
				payload = new String(responseData, contentCharset);
				if (contentType.contains("html")) {
					payload = HtmlStrip.getInnerText(payload, "body");
					payload = HtmlStrip.getInnerText(payload, "html");
					payload = payload.replaceAll("<[^>]+>", " "); // remove all tags
					payload = payload.replaceAll("( *\\r?\\n *)+", "\n"); // simplify new-lines
					payload = payload.replaceAll("[ ]{2,}", " "); // simplify spaces
					log.debug(payload);
				}

			}
			return connection.getURL() + "\n" + "> " + connection.getResponseCode() + " "
					+ connection.getResponseMessage() + "\ncontent type: " + contentType + "\n" + payload;
		} catch (Exception e) {
			return "Unknown error " + e.getClass().getSimpleName() + "\n" + e.getMessage();
		}
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " " + connection.getURL();
	}

}
