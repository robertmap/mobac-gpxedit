package mobac.utilities.beanshell;

import mobac.mapsources.MapSourceTools;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.DirectoryManager;
import mobac.program.interfaces.MapSpace;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.swing.JOptionPane;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.SecureRandom;

public class Tools {

	public static final SecureRandom RND = new SecureRandom();
	public static final MapSpace OSM_MERCATOR = MercatorPower2MapSpace.INSTANCE_256;

	/**
	 * Calculates latitude and longitude of the upper left corner of the specified
	 * tile of <code>OSM_MERCATOR</code> regarding the zoom level specified by
	 * <code>zoom</code>.
	 *
	 * @param zoom
	 * @param tilex
	 * @param tiley
	 * @param tilex
	 *            horizontal tile number
	 * @param tiley
	 *            vertical tile number
	 * @return <code>double[] {lon_min, lat_max, lon_max, lat_min}</code>
	 */
	@MethodDescription("Calculates latitude and longitude of the upper left corner of the specified "
			+ "tile of OSM_MERCATOR regarding the zoom level specified by zoom. Returns an array "
			+ "of fouzr double values {lon_min, lat_max, lon_max, lat_min}")
	public static double[] calculateTileLatLon(int zoom, int tilex, int tiley) {
		return MapSourceTools.calculateLatLon(OSM_MERCATOR, zoom, tilex, tiley);
	}

	@MethodDescription("Converts an horizontal tile number on a certain zoom level "
			+ "into the corespondent longitude")
	public static double xTileToLon(int x, int zoom) {
		return OSM_MERCATOR.cXToLon(x, zoom);
	}

	@MethodDescription("Converts an vertical tile number on a certain zoom level " + "into the corespondent latitude")
	public static double yTileToLat(int y, int zoom) {
		return OSM_MERCATOR.cYToLat(y, zoom);
	}

	@MethodDescription("Returns a random value. Range [0..<b>max</b>]")
	public static int getRandomInt(int max) {
		return RND.nextInt(max + 1);
	}

	@MethodDescription("Converts a tile numer on a certain zoom level into a quad tree coordinate")
	public static String encodeQuadTree(int zoom, int tilex, int tiley) {
		return MapSourceTools.encodeQuadTree(zoom, tilex, tiley);
	}

	@MethodDescription("Returns a byte array of length <b>length</b> filled with random data.")
	public static byte[] getRandomByteArray(int length) {
		byte[] buf = new byte[length];
		RND.nextBytes(buf);
		return buf;
	}

	@MethodDescription("Encodes the <b>binaryData</b> byte array to a " + "base64 String without line breaks")
	public static String encodeBase64(byte[] binaryData) {
		return new String(Base64.encodeBase64(binaryData));
	}

	@MethodDescription("Decodes an base64 encoded String to a byte array")
	public static byte[] decodeBase64(String base64String) {
		return Base64.decodeBase64(base64String);
	}

	@MethodDescription("Encodes the <b>binaryData</b> byte array to a hexadecimal String "
			+ "without line breaks, leading 0x and spaces")
	public static String encodeHex(byte[] binaryData) {
		return Hex.encodeHexString(binaryData);
	}

	@MethodDescription("Decodes an hexadecimal encoded String to a byte array. The string have to "
			+ "contain only the hexadecimal encoded nibbles.")
	public static byte[] decodeHex(String hexString) throws DecoderException {
		return Hex.decodeHex(hexString.toCharArray());
	}

	@MethodDescription("Get the configured directory java.io.File object")
	public static File getDirectory(String dirType) {
		switch (dirType) {
			case "atlasProfilesDir" :
				return DirectoryManager.atlasProfilesDir;
			case "currentDir" :
				return DirectoryManager.currentDir;
			case "mapSourcesDir" :
				return DirectoryManager.mapSourcesDir;
			case "programDir" :
				return DirectoryManager.programDir;
			case "tempDir" :
				return DirectoryManager.tempDir;
			case "tileStoreDir" :
				return DirectoryManager.tileStoreDir;
			case "toolsDir" :
				return DirectoryManager.toolsDir;
			case "userAppDataDir" :
				return DirectoryManager.mobacUserAppDataDir;
			case "userHomeDir" :
				return DirectoryManager.userHomeDir;
			case "userSettingsDir" :
				return DirectoryManager.userSettingsDir;
		}
		throw new IllegalArgumentException("Invalid dirtype argument: \"" + dirType + "\"");
	}

	@MethodDescription("Open an dialog box and display the message (useful for debugging, available since MOBAc 2.2.2)")
	public static void alert(Object message) {
		JOptionPane.showMessageDialog(null, message);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface MethodDescription {
		String value();
	}

}
