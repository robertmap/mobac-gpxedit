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
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;
import mobac.exceptions.AbortedByUserException;
import mobac.gui.panels.JProfilesPanel;
import mobac.program.DirectoryManager;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.AtlasObject;
import mobac.utilities.I18nUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A profile is a saved atlas. The available profiles ({@link Profile}
 * instances) are visible in the <code>profilesCombo</code> in the
 * {@link JProfilesPanel}.
 */
public class Profile implements Comparable<Profile> {

	public static final String PROFILE_NAME_REGEX = "[\\w _-]+";
	public static final String PROFILE_FILENAME_PREFIX = "mobac-profile-";
	public static final Pattern PROFILE_FILENAME_PATTERN = Pattern
			.compile(PROFILE_FILENAME_PREFIX + "(" + PROFILE_NAME_REGEX + ").xml");
	public static final Profile DEFAULT = new Profile();
	private static final Logger log = LoggerFactory.getLogger(Profile.class);
	private static final Vector<Profile> profiles = new Vector<>();

	private final File file;
	private final String name;

	/**
	 * Load a profile by it's name
	 *
	 * @param name
	 */
	public Profile(String name) {
		this(new File(DirectoryManager.atlasProfilesDir, getProfileFileName(name)), name);
	}

	/**
	 * Default profile
	 */
	protected Profile() {
		this(new File(DirectoryManager.atlasProfilesDir, "mobac-profile.xml"), "");
	}

	protected Profile(File file, String name) {
		super();
		this.file = file;
		this.name = name;
	}

	/**
	 * Profiles management method
	 */
	public static void updateProfiles() {
		File profilesDir = DirectoryManager.atlasProfilesDir;
		final Set<Profile> deletedProfiles = new HashSet<>(profiles);
		profilesDir.list((dir, fileName) -> {
			Matcher m = PROFILE_FILENAME_PATTERN.matcher(fileName);
			if (m.matches()) {
				String profileName = m.group(1);
				Profile profile = new Profile(new File(dir, fileName), profileName);
				if (!deletedProfiles.remove(profile)) {
					profiles.add(profile);
				}
			}
			return false;
		});
		for (Profile p : deletedProfiles) {
			profiles.remove(p);
		}
		Collections.sort(profiles);
	}

	/**
	 * Profiles management method
	 */
	public static Vector<Profile> getProfiles() {
		updateProfiles();
		return profiles;
	}

	public static boolean checkAtlas(AtlasInterface atlasInterface) {
		return checkAtlasObject(atlasInterface);
	}

	public static String getProfileFileName(String profileName) {
		return PROFILE_FILENAME_PREFIX + profileName + ".xml";
	}

	private static boolean checkAtlasObject(Object o) {
		boolean result = false;
		if (o instanceof AtlasObject) {
			result |= ((AtlasObject) o).checkData();
		}
		if (o instanceof Iterable<?>) {
			Iterable<?> it = (Iterable<?>) o;
			for (Object ao : it) {
				result |= checkAtlasObject(ao);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return name;
	}

	public File getFile() {
		return file;
	}

	public String getName() {
		return name;
	}

	public boolean exists() {
		return file.isFile();
	}

	public void delete() {
		if (!file.delete()) {
			file.deleteOnExit();
		}
	}

	public int compareTo(Profile o) {
		return file.compareTo(o.file);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Profile)) {
			return false;
		}
		Profile p = (Profile) obj;
		return file.equals(p.file);
	}

	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
		return -1;
	}

	public void save(AtlasInterface atlasInterface) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(Atlas.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		try (FileOutputStream fo = new FileOutputStream(file)) {
			m.marshal(atlasInterface, fo);
		} catch (IOException e) {
			throw new JAXBException(e);
		}
	}

	public AtlasInterface load() throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(Atlas.class);
		Unmarshaller um = context.createUnmarshaller();
		AtomicBoolean loadAborted = new AtomicBoolean(false);
		um.setEventHandler(new ValidationEventHandler() {

			public boolean handleEvent(ValidationEvent event) {
				ValidationEventLocator loc = event.getLocator();
				String fileName = file.getName();
				String message = event.getMessage();
				if (message == null) {
					// No message - try to find an error message in the linked Exceptions
					Throwable ex = event.getLinkedException();
					while (ex instanceof InvocationTargetException) {
						ex = ex.getCause();
					}
					if (ex != null) {
						message = ex.getMessage();
					} else {
						message = "?";
					}
				}
				int ret = JOptionPane.showConfirmDialog(null,
						String.format(I18nUtils.localizedStringForKey("msg_error_load_atlas_profile"), message,
								fileName, loc.getLineNumber(), loc.getColumnNumber()),
						I18nUtils.localizedStringForKey("msg_error_load_atlas_profile_title"),
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
				log.error(event.toString());
				boolean continueLoading = (ret == JOptionPane.YES_OPTION);
				if (!continueLoading) {
					loadAborted.set(true);
				}
				return continueLoading;
			}
		});
		try {
			AtlasInterface newAtlas = (AtlasInterface) um.unmarshal(file);
			return newAtlas;
		} catch (Exception e) {
			if (loadAborted.get()) {
				throw new AbortedByUserException();
			}
			throw new JAXBException(e.getMessage(), e);
		}
	}
}
