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

    @Test
    void testGetFileNameByUTF8OnlyNumbers() {
        String result = EncodeUtil.getFileNameByUTF8("12345.txt");
        assertEquals("12345.txt", result);
    }

    @Test
    void testGetFileNameByUTF8OnlyExtension() {
        String result = EncodeUtil.getFileNameByUTF8(".gitignore");
        assertEquals(".gitignore", result);
    }

    @Test
    void testGetFileNameByUTF8NoExtension() {
        String result = EncodeUtil.getFileNameByUTF8("README");
        assertEquals("README", result);
    }

    @Test
    void testGetFileNameByUTF8MultipleDots() {
        String result = EncodeUtil.getFileNameByUTF8("archive.tar.gz");
        assertTrue(result.contains("archive"));
        assertTrue(result.contains("tar"));
        assertTrue(result.contains("gz"));
    }

    @Test
    void testGetFileNameByUTF8UnicodeSpecialChars() {
        String result = EncodeUtil.getFileNameByUTF8("日本語ファイル.txt");
        assertTrue(result.contains("%"));
        assertTrue(result.contains("txt"));
    }

    @Test
    void testGetFileNameByUTF8SlashEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("file/name.txt");
        assertTrue(result.contains("%2F"));
    }

    @Test
    void testGetFileNameByUTF8BackslashEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("file\\name.txt");
        assertTrue(result.contains("%5C"));
    }

    @Test
    void testGetFileNameByUTF8QuestionMarkEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("what?.txt");
        assertTrue(result.contains("%3F"));
    }

    @Test
    void testGetFileNameByUTF8AmpersandEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a&b.txt");
        assertTrue(result.contains("%26"));
    }

    @Test
    void testGetFileNameByUTF8EqualsSignEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a=b.txt");
        assertTrue(result.contains("%3D"));
    }

    @Test
    void testGetFileNameByUTF8ColonEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a:b.txt");
        assertTrue(result.contains("%3A"));
    }

    @Test
    void testGetFileNameByUTF8SemicolonEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a;b.txt");
        assertTrue(result.contains("%3B"));
    }

    @Test
    void testGetFileNameByUTF8AtSignEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("user@host.txt");
        assertTrue(result.contains("%40"));
    }

    @Test
    void testGetFileNameByUTF8TabEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("file\tname.txt");
        assertTrue(result.contains("%09"));
    }

    @Test
    void testGetFileNameByUTF8NewlineEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("file\nname.txt");
        assertTrue(result.contains("%0A"));
    }

    @Test
    void testGetFileNameByUTF8SingleChar() {
        String result = EncodeUtil.getFileNameByUTF8("a");
        assertEquals("a", result);
    }

    @Test
    void testGetFileNameByUTF8WhitespaceOnly() {
        String result = EncodeUtil.getFileNameByUTF8("   ");
        assertEquals("%20%20%20", result);
    }

    @Test
    void testGetFileNameByUTF8LeadingTrailingSpaces() {
        String result = EncodeUtil.getFileNameByUTF8("  test.txt  ");
        assertTrue(result.startsWith("%20%20"));
        assertTrue(result.endsWith("%20%20"));
    }

    @Test
    void testGetFileNameByUTF8BracketsEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("[file].txt");
        assertTrue(result.contains("%5B"));
        assertTrue(result.contains("%5D"));
    }

    @Test
    void testGetFileNameByUTF8BracesEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("{file}.txt");
        assertTrue(result.contains("%7B"));
        assertTrue(result.contains("%7D"));
    }

    @Test
    void testGetFileNameByUTF8PipeEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a|b.txt");
        assertTrue(result.contains("%7C"));
    }

    @Test
    void testGetFileNameByUTF8CaretEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a^b.txt");
        assertTrue(result.contains("%5E"));
    }

    @Test
    void testGetFileNameByUTF8BacktickEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("a`b.txt");
        assertTrue(result.contains("%60"));
    }

    @Test
    void testGetFileNameByUTF8TildeMayBeEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("~file.txt");
        assertNotNull(result);
        assertTrue(result.contains("file"));
        assertTrue(result.contains("txt"));
    }

    @Test
    void testGetFileNameByUTF8HyphenUnderscorePreserved() {
        String result = EncodeUtil.getFileNameByUTF8("my-file_v2.txt");
        assertEquals("my-file_v2.txt", result);
    }

    @Test
    void testGetFileNameByUTF8DotPreserved() {
        String result = EncodeUtil.getFileNameByUTF8(".hidden");
        assertEquals(".hidden", result);
    }

    @Test
    void testGetFileNameByUTF8EmojiEncoded() {
        String emojiName = "\uD83D\uDE00file.txt";
        String result = EncodeUtil.getFileNameByUTF8(emojiName);
        assertNotNull(result);
        assertTrue(result.contains("txt"));
        assertTrue(result.contains("%"), "Emoji should be URL-encoded");
    }

    @Test
    void testGetFileNameByUTF8ArabicEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("ملف.txt");
        assertTrue(result.contains("%"));
    }

    @Test
    void testGetFileNameByUTF8CyrillicEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("файл.txt");
        assertTrue(result.contains("%"));
    }

    @Test
    void testGetFileNameByUTF8LongName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("a");
        }
        sb.append(".txt");
        String longName = sb.toString();
        String result = EncodeUtil.getFileNameByUTF8(longName);
        assertTrue(result.endsWith(".txt"));
        assertTrue(result.length() >= longName.length());
    }

    @Test
    void testGetFileNameByUTF8PercentSignEncoded() {
        String result = EncodeUtil.getFileNameByUTF8("100%.txt");
        assertTrue(result.contains("%25"));
    }

    @Test
    void testGetFileNameByUTF8PlusSignReplacedWith20() {
        String result = EncodeUtil.getFileNameByUTF8("a+b=c.txt");
        assertTrue(result.contains("%2B"));
        assertTrue(result.contains("%3D"));
        assertFalse(result.contains("+"));
    }

}