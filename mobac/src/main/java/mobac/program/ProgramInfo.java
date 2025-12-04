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
package mobac.program;

import mobac.Main;
import mobac.utilities.GUIExceptionHandler;

import java.io.InputStream;
import java.util.Properties;

public class ProgramInfo {

	public static String PROG_NAME = "Mobile Atlas Creator";
	public static String PROG_NAME_SHORT = "MOBAC";

	private static String VERSION = "unknown";
    private static String GIT_BRANCH_NAME = "unknown";
	private static String GIT_COMMIT_HASH;
    private static String GIT_COMMIT_TIME;
	private static String userAgent = "";

	/**
	 * Show or hide the detailed revision info in the main windows title
	 */
	private static boolean titleHideCommit = false;

	public static void initialize() {

		try (InputStream in = Main.class.getResourceAsStream("mobac.properties")) {
			Properties props = new Properties();
			props.load(in);
			titleHideCommit = Boolean.parseBoolean(props.getProperty("mobac.revision.hide", "false"));
			System.getProperties().putAll(props);
		} catch (Exception e) {
			String msg = "Error reading mobac.properties";
			GUIExceptionHandler.processFatalExceptionSimpleDialog(msg, e);
		}
		try (InputStream in = Main.class.getResourceAsStream("mobac-rev.properties")) {
			if (in != null) {
				Properties props = new Properties();
				props.load(in);
                GIT_BRANCH_NAME = props.getProperty("mobac.gitBranchName", GIT_BRANCH_NAME);
                GIT_COMMIT_HASH = props.getProperty("mobac.gitCommitHash");
                GIT_COMMIT_TIME = props.getProperty("mobac.gitCommitTime");
                GIT_BRANCH_NAME = props.getProperty("mobac.gitBranchName");
				VERSION = props.getProperty("mobac.version", VERSION);
			}
		} catch (Exception e) {
			Logging.LOG.error("Error reading mobac-rev.properties", e);
		}
		userAgent = PROG_NAME_SHORT + "/" + (getVersion().replaceAll(" ", "_"));
	}

	public static String getVersion() {
		if (VERSION != null) {
			return VERSION;
		}
		return "UNKNOWN";
	}

	public static String getRevisionStr() {
		if (GIT_COMMIT_HASH == null) {
            return GIT_BRANCH_NAME;
        }
        return String.format("%s (%s)", GIT_COMMIT_HASH, GIT_BRANCH_NAME);
	}

	public static String getVersionTitle() {
		String title = PROG_NAME;
		if (PROG_NAME_SHORT != null) {
			title += " (" + PROG_NAME_SHORT + ") ";
		} else {
			title += " ";
		}
		if (VERSION != null) {
			title += getVersion();
		} else {
			title += "unknown version";
		}
		return title;
	}

	public static String getCompleteTitle() {
		String title = getVersionTitle();
		if (!titleHideCommit) {
			title += " (" + GIT_COMMIT_HASH + ")";
		}
		return title;
	}

	public static String getUserAgent() {
		return userAgent;
	}

    public static String getGitCommitTime() {
        return GIT_COMMIT_TIME;
    }
}
