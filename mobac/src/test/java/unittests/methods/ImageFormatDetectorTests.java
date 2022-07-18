package unittests.methods;

import mobac.program.model.TileImageType;
import mobac.utilities.imageio.ImageFormatDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ImageFormatDetectorTests {

    @Test
    public void testUnknown() {
        for (int i = 0; i < 20; i++) {
            assertNull(ImageFormatDetector.getImageType(new byte[i]));
        }
    }

    @Test
    public void testWEBP() {
        byte[] rawData = {
                (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46, (byte) 0xAC, (byte) 0x43,
                (byte) 0x00, (byte) 0x00, (byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50,
                (byte) 0x56, (byte) 0x50, (byte) 0x38, (byte) 0x20, (byte) 0xA0, (byte) 0x43,
                (byte) 0x00, (byte) 0x00, (byte) 0xF0, (byte) 0xAD, (byte) 0x01, (byte) 0x9D,
                (byte) 0x01, (byte) 0x2A, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                (byte) 0x3E, (byte) 0x6D
        };


        TileImageType imageType = ImageFormatDetector.getImageType(rawData);
        assertEquals(TileImageType.WEBP, imageType);
    }

    @Test
    public void testJPEG() {
        byte[] rawData = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, (byte) 0x00, (byte) 0x10,
                (byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46, (byte) 0x00, (byte) 0x01,
                (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x60, (byte) 0x00, (byte) 0x60,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xDB, (byte) 0x00, (byte) 0x43,
                (byte) 0x00, (byte) 0x03, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x02,
                (byte) 0x02, (byte) 0x03
        };

        TileImageType imageType = ImageFormatDetector.getImageType(rawData);
        assertEquals(TileImageType.JPG, imageType);
    }

    @Test
    public void testPNG() {

        byte[] rawData = {
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A,
                (byte) 0x1A, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D,
                (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52, (byte) 0x00, (byte) 0x00,
                (byte) 0x01, (byte) 0x32, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x32,
                (byte) 0x08, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x79,
                (byte) 0xC6, (byte) 0x5C
        };

        TileImageType imageType = ImageFormatDetector.getImageType(rawData);
        assertEquals(TileImageType.PNG, imageType);
    }

}
