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
        assertTrue(tfu.matcherFileName("file..txt"));
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
        assertFalse(tfu.matcherFolderName(""));
    }

    @Test
    void testMatcherFileNameEmptyString() {
        assertFalse(tfu.matcherFileName(""));
    }

    @Test
    void testMatcherFolderNameJustIllegalChar() {
        assertFalse(tfu.matcherFolderName("|"));
        assertFalse(tfu.matcherFolderName(":"));
        assertFalse(tfu.matcherFolderName("*"));
    }

    @Test
    void testMatcherNameRejectsControlCharacters() {
        assertFalse(tfu.matcherFileName("file\u0000.txt"));
        assertFalse(tfu.matcherFolderName("folder\u0007name"));
        assertFalse(tfu.matcherFileName("file\u007f.txt"));
    }

    @Test
    void testMatcherNameRejectsLeadingOrTrailingSpaceAndDot() {
        assertFalse(tfu.matcherFileName(" leading.txt"));
        assertFalse(tfu.matcherFileName("trailing "));
        assertFalse(tfu.matcherFileName(".hidden.txt"));
        assertFalse(tfu.matcherFileName("file."));
        assertFalse(tfu.matcherFolderName(".hidden"));
    }

    @Test
    void testMatcherNameRejectsWindowsReservedNames() {
        assertFalse(tfu.matcherFileName("CON"));
        assertFalse(tfu.matcherFileName("con.txt"));
        assertFalse(tfu.matcherFileName("COM1"));
        assertFalse(tfu.matcherFileName("LPT9"));
        assertFalse(tfu.matcherFolderName("NUL"));
    }

    @Test
    void testMatcherNameRejectsTooLongName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append('a');
        }
        assertFalse(tfu.matcherFileName(sb.toString()));
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