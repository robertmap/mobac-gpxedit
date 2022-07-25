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
package mobac.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NameNotFoundException;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OSUtilities {

	public static final boolean IS_PLATFORM_OSX = isPlatformOsx();
	static Logger log = LoggerFactory.getLogger(OSUtilities.class);

	private static boolean isPlatformOsx() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("mac os x");
	}

	public static OperatingSystem detectOs() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("windows")) {
			return OperatingSystem.Windows;
		}
		if (osName.contains("linux")) {
			return OperatingSystem.Linux;
		}
		if (osName.contains("mac os x")) {
			return OperatingSystem.MacOsX;
		}

		return OperatingSystem.Unknown;
	}

	public static void openFolderBrowser(File directory) throws NameNotFoundException {
		if (!directory.isDirectory()) {
			throw new NameNotFoundException("Directory does not exist or is not a directory");
		}
		try {
			Desktop.getDesktop().open(directory.getAbsoluteFile().getCanonicalFile());
		} catch (Exception e) {
			StringBuilder sb = new StringBuilder(512);
			sb.append(I18nUtils.localizedStringForKey("msg_environment_failed_open_output"));
			sb.append(e.getMessage());
			sb.append("</html>");
			String msg = sb.toString();
			JOptionPane.showMessageDialog(null, msg,
					I18nUtils.localizedStringForKey("msg_environment_failed_open_output_title"),
					JOptionPane.ERROR_MESSAGE);
			log.error(msg, e);
		}

	}

	/**
	 * Reads the Linux distribution name (last line) from the first file that
	 * matches the pattern
	 *
	 * <pre>
	 * /etc/*-release
	 * </pre>
	 *
	 * @return Linux distribution name or <code>null</code>
	 */
	public static String getLinuxDistributionName() {
		try {
			Path etcDir = Paths.get("/etc");
			if (!Files.isDirectory(etcDir)) {
				return null;
			}
			final Pattern pattern = Pattern.compile(".*-release");
			List<Path> releaseFileList = Files.walk(etcDir)
					.filter(f -> pattern.matcher(f.getFileName().toString()).matches()).collect(Collectors.toList());
			if (releaseFileList.isEmpty()) {
				return null;
			}
			Path releaseFile;
			if (releaseFileList.size() > 1) {
				Optional<Path> optional = releaseFileList.stream()
						.filter(f -> f.getFileName().toString().contains("lsb")).findFirst();
				if (optional.isPresent()) {
					releaseFile = optional.get();
				} else {
					releaseFile = releaseFileList.get(0);
				}
			} else {
				releaseFile = releaseFileList.get(0);
			}
			List<String> lines = Files.readAllLines(releaseFile);
			String result = lines.stream().map(s -> {
				s = s.trim();
				int index = s.indexOf('=');
				if (index > 0) {
					s = s.substring(index + 1);
				}
				if (s.startsWith("\"") && s.endsWith("\"")) {
					s = s.substring(1, s.length() - 2);
				}
				return s;
			}).collect(Collectors.joining(", "));
			return result;
		} catch (Exception e) {
			log.trace("Failed to read Linux release file", e);
			return null;
		}
	}

	public enum OperatingSystem {
		Windows, Linux, MacOsX, Solaris, Unknown
	}

}
