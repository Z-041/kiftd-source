package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PictureInfoTest {

    @Test
    void testDefaultValuesAreNull() {
        PictureInfo pictureInfo = new PictureInfo();
        assertNull(pictureInfo.getFileName());
        assertNull(pictureInfo.getUrl());
    }

    @Test
    void testSetAndGetAllFields() {
        PictureInfo pictureInfo = new PictureInfo();
        String fileName = "photo.jpg";
        String url = "https://example.com/photo.jpg";

        pictureInfo.setFileName(fileName);
        pictureInfo.setUrl(url);

        assertEquals(fileName, pictureInfo.getFileName());
        assertEquals(url, pictureInfo.getUrl());
    }

    @Test
    void testSetNullValues() {
        PictureInfo pictureInfo = new PictureInfo();
        pictureInfo.setFileName("test.jpg");
        pictureInfo.setUrl("http://test.com");

        pictureInfo.setFileName(null);
        pictureInfo.setUrl(null);

        assertNull(pictureInfo.getFileName());
        assertNull(pictureInfo.getUrl());
    }

    @Test
    void testEmptyStringValues() {
        PictureInfo pictureInfo = new PictureInfo();
        pictureInfo.setFileName("");
        pictureInfo.setUrl("");

        assertEquals("", pictureInfo.getFileName());
        assertEquals("", pictureInfo.getUrl());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : PictureInfo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(2, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : PictureInfo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testLongStringValues() {
        PictureInfo pictureInfo = new PictureInfo();
        String longFileName = "a".repeat(5000) + ".jpg";
        String longUrl = "https://example.com/" + "a".repeat(5000);

        pictureInfo.setFileName(longFileName);
        pictureInfo.setUrl(longUrl);

        assertEquals(longFileName, pictureInfo.getFileName());
        assertEquals(longUrl, pictureInfo.getUrl());
    }

    @Test
    void testSpecialCharacterValues() {
        PictureInfo pictureInfo = new PictureInfo();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        pictureInfo.setFileName(specialChars + ".jpg");
        pictureInfo.setUrl("https://example.com/" + specialChars);

        assertEquals(specialChars + ".jpg", pictureInfo.getFileName());
        assertEquals("https://example.com/" + specialChars, pictureInfo.getUrl());
    }

    @Test
    void testUnicodeValues() {
        PictureInfo pictureInfo = new PictureInfo();
        String unicode = "中文测试_日本語テスト_한국어테스트_😀";

        pictureInfo.setFileName(unicode + ".jpg");
        pictureInfo.setUrl("https://example.com/" + unicode);

        assertEquals(unicode + ".jpg", pictureInfo.getFileName());
        assertEquals("https://example.com/" + unicode, pictureInfo.getUrl());
    }

    @Test
    void testMultipleSetAndGet() {
        PictureInfo pictureInfo = new PictureInfo();

        for (int i = 0; i < 10; i++) {
            pictureInfo.setFileName("photo-" + i + ".jpg");
            pictureInfo.setUrl("http://example.com/photo-" + i + ".jpg");
            assertEquals("photo-" + i + ".jpg", pictureInfo.getFileName());
            assertEquals("http://example.com/photo-" + i + ".jpg", pictureInfo.getUrl());
        }
    }

    @Test
    void testFieldsAreIndependent() {
        PictureInfo pictureInfo = new PictureInfo();
        pictureInfo.setFileName("file1.jpg");
        pictureInfo.setUrl("http://example.com/file1.jpg");

        pictureInfo.setFileName("file2.jpg");
        assertEquals("file2.jpg", pictureInfo.getFileName());
        assertEquals("http://example.com/file1.jpg", pictureInfo.getUrl());
    }
}
