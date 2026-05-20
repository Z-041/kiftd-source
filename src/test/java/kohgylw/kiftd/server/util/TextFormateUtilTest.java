package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TextFormateUtilTest {

    private static TextFormateUtil tfu;

    @BeforeAll
    static void setup() {
        tfu = TextFormateUtil.instance();
    }

    @Test
    void testMatcherFolderNameValid() {
        assertTrue(tfu.matcherFolderName("MyFolder"));
        assertTrue(tfu.matcherFolderName("Folder123"));
        assertTrue(tfu.matcherFolderName("中文文件夹"));
        assertTrue(tfu.matcherFolderName("My_Folder-Name"));
    }

    @Test
    void testMatcherFolderNameInvalid() {
        assertFalse(tfu.matcherFolderName("Folder|Name"));
        assertFalse(tfu.matcherFolderName("Folder/Name"));
        assertFalse(tfu.matcherFolderName("Folder*Name"));
        assertFalse(tfu.matcherFolderName("Folder?Name"));
        assertFalse(tfu.matcherFolderName("Folder&Name"));
        assertFalse(tfu.matcherFolderName("Folder$Name"));
        assertFalse(tfu.matcherFolderName("Folder:Name"));
    }

    @Test
    void testMatcherFileNameValid() {
        assertTrue(tfu.matcherFileName("document.txt"));
        assertTrue(tfu.matcherFileName("my-photo.jpg"));
        assertTrue(tfu.matcherFileName("测试文件.pdf"));
        assertTrue(tfu.matcherFileName("file_v1.2.tar.gz"));
    }

    @Test
    void testMatcherFileNameInvalid() {
        assertFalse(tfu.matcherFileName("file|name.txt"));
        assertFalse(tfu.matcherFileName("file/name.txt"));
        assertFalse(tfu.matcherFileName("file*name.txt"));
        assertFalse(tfu.matcherFileName("file?name.txt"));
        assertFalse(tfu.matcherFileName("file&name.txt"));
        assertFalse(tfu.matcherFileName("file:name.txt"));
    }

    @Test
    void testMatcherFolderNameEmptyString() {
        assertTrue(tfu.matcherFolderName(""));
    }

    @Test
    void testMatcherFileNameEmptyString() {
        assertTrue(tfu.matcherFileName(""));
    }

    @Test
    void testMatcherFolderNameJustIllegalChar() {
        assertFalse(tfu.matcherFolderName("|"));
        assertFalse(tfu.matcherFolderName(":"));
        assertFalse(tfu.matcherFolderName("*"));
    }

    @Test
    void testHasEscapesWithBackslash() {
        assertTrue(tfu.hasEscapes("test\\path"));
        assertTrue(tfu.hasEscapes("\\"));
        assertTrue(tfu.hasEscapes("start\\end"));
    }

    @Test
    void testHasEscapesWithoutBackslash() {
        assertFalse(tfu.hasEscapes("normal/path"));
        assertFalse(tfu.hasEscapes("normal path"));
        assertFalse(tfu.hasEscapes(""));
    }

    @Test
    void testHasEscapesOnlyBackslash() {
        assertTrue(tfu.hasEscapes("\\"));
    }

}