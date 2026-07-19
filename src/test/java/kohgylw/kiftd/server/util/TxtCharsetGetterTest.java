package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TxtCharsetGetterTest {

    private final TxtCharsetGetter getter = new TxtCharsetGetter();

    @Test
    void testGetTxtCharset_AsciiText_InputStream() throws Exception {
        String text = "Hello World! This is a test.";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII));
        String result = getter.getTxtCharset(in);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_AsciiText_ByteArray() throws Exception {
        String text = "Hello World! This is a test.";
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        String result = getter.getTxtCharset(bytes, 0, bytes.length);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_UTF8Chinese_InputStream() throws Exception {
        String text = "这是一个中文测试文本。";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        String result = getter.getTxtCharset(in);
        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("utf") || result.toLowerCase().contains("gb") || 
                   result.toLowerCase().contains("ascii"));
    }

    @Test
    void testGetTxtCharset_UTF8Chinese_ByteArray() throws Exception {
        String text = "这是一个中文测试文本。";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        String result = getter.getTxtCharset(bytes, 0, bytes.length);
        assertNotNull(result);
    }

    @Test
    void testGetTxtCharset_EmptyStream_InputStream() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        String result = getter.getTxtCharset(in);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_EmptyArray_ByteArray() throws Exception {
        byte[] bytes = new byte[0];
        String result = getter.getTxtCharset(bytes, 0, 0);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_LongAsciiText_InputStream() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("The quick brown fox jumps over the lazy dog. ");
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.US_ASCII));
        String result = getter.getTxtCharset(in);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_LongAsciiText_ByteArray() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("The quick brown fox jumps over the lazy dog. ");
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.US_ASCII);
        String result = getter.getTxtCharset(bytes, 0, bytes.length);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_SpecialChars_InputStream() throws Exception {
        String text = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII));
        String result = getter.getTxtCharset(in);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_NumericOnly_InputStream() throws Exception {
        String text = "1234567890";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII));
        String result = getter.getTxtCharset(in);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_JapaneseText_InputStream() throws Exception {
        String text = "日本語のテスト";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        String result = getter.getTxtCharset(in);
        assertNotNull(result);
    }

    @Test
    void testGetTxtCharset_KoreanText_InputStream() throws Exception {
        String text = "한국어 테스트";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        String result = getter.getTxtCharset(in);
        assertNotNull(result);
    }

    @Test
    void testGetTxtCharset_ByteArrayWithOffset() throws Exception {
        String text = "xxxHello Worldxxx";
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        String result = getter.getTxtCharset(bytes, 3, 11);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_ByteArrayWithFullOffset() throws Exception {
        String text = "Hello World";
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        String result = getter.getTxtCharset(bytes, 0, bytes.length);
        assertEquals("ASCII", result);
    }

    @Test
    void testGetTxtCharset_UTF8BOM_InputStream() throws Exception {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String text = "Hello";
        byte[] combined = new byte[bom.length + text.getBytes(StandardCharsets.UTF_8).length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(text.getBytes(StandardCharsets.UTF_8), 0, combined, bom.length, text.getBytes(StandardCharsets.UTF_8).length);
        InputStream in = new ByteArrayInputStream(combined);
        String result = getter.getTxtCharset(in);
        assertNotNull(result);
    }

    @Test
    void testGetTxtCharset_MixedContent_InputStream() throws Exception {
        String text = "Hello World 你好世界";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        String result = getter.getTxtCharset(in);
        assertNotNull(result);
    }

    @Test
    void testGetTxtCharset_ReturnsString() throws Exception {
        String text = "test";
        InputStream in = new ByteArrayInputStream(text.getBytes());
        String result = getter.getTxtCharset(in);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetTxtCharset_ByteArrayReturnsString() throws Exception {
        byte[] bytes = "test".getBytes();
        String result = getter.getTxtCharset(bytes, 0, bytes.length);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
