package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EncodeUtilTest {

    @Test
    void testGetFileNameByUTF8NormalName() {
        String result = EncodeUtil.getFileNameByUTF8("test.txt");
        assertTrue(result.contains("test"));
        assertTrue(result.contains("txt"));
    }

    @Test
    void testGetFileNameByUTF8ChineseName() {
        String result = EncodeUtil.getFileNameByUTF8("中文文件.txt");
        assertTrue(result.contains("%"));
        assertTrue(result.contains("txt"));
    }

    @Test
    void testGetFileNameByUTF8SpacesReplaced() {
        String result = EncodeUtil.getFileNameByUTF8("file with spaces.txt");
        assertEquals("file%20with%20spaces.txt", result);
    }

    @Test
    void testGetFileNameByUTF8PlusSignEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a+b.txt");
        assertTrue(result.contains("%2B"));
    }

    @Test
    void testGetFileNameByUTF8EmptyString() {
        String result = EncodeUtil.getFileNameByUTF8("");
        assertEquals("", result);
    }

    @Test
    void testGetFileNameByUTF8NullInput() {
        assertThrows(NullPointerException.class, () -> {
            EncodeUtil.getFileNameByUTF8(null);
        });
    }

    @Test
    void testGetFileNameByUTF8SpecialChars() {
        String result = EncodeUtil.getFileNameByUTF8("hello#world$test.txt");
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));
    }

    @Test
    void testGetFileNameByUTF8DoesNotReturnOriginal() {
        String result = EncodeUtil.getFileNameByUTF8("hello world.txt");
        assertNotEquals("hello world.txt", result);
    }

}