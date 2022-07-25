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
package mobac.program.commandline;

import mobac.program.download.MobacTrustManager;

public class PrintPublicKeyHash extends CommandLineEmpty {

	public static final String CMD_ARG = "keyhash";

	private String serverUrl;

	public PrintPublicKeyHash(String serverUrl) {
		super();
		this.serverUrl = serverUrl;
	}

	@Override
	public boolean showSplashScreen() {
		return false;
	}

	@Override
	public boolean showMainGUI() {
		return false;
	}

	@Override
	public void afterBasicInitialization() {
		serverUrl = serverUrl.toLowerCase();
		if (!serverUrl.startsWith("https://")) {
			serverUrl = "https://" + serverUrl;
		}
		String hash;
		try {
			hash = MobacTrustManager.getServerPublicKeyHash(serverUrl);
			System.out.println("Server:          " + serverUrl);
			System.out.println("Public Key Hash: " + hash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

}
