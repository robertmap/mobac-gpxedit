package unittests.methods;

import mobac.utilities.I18nUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class I18nTests {

	@Test
	public void testi18n() {
		File file = new File("/tmp/test");
		String s1 = I18nUtils.localizedStringForKey("msg_custom_map_failed_open_source_zip", "name", "####");
		String s2 = I18nUtils.localizedStringForKey("msg_custom_map_failed_open_source_zip", "name", file);
		assertEquals(s1.replace("####", file.toString()), s2);
	}
}
