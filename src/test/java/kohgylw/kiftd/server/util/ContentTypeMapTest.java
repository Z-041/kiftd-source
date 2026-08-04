package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ContentTypeMapTest {

    private final ContentTypeMap map = new ContentTypeMap();

    @Test
    void testHtml() {
        assertEquals("text/html", map.getContentType(".html"));
    }

    @Test
    void testCss() {
        assertEquals("text/css", map.getContentType(".css"));
    }

    @Test
    void testJs() {
        assertEquals("application/javascript", map.getContentType(".js"));
    }

    @Test
    void testJpg() {
        assertEquals("image/jpeg", map.getContentType(".jpg"));
        assertEquals("image/jpeg", map.getContentType(".jpeg"));
    }

    @Test
    void testPng() {
        assertEquals("image/png", map.getContentType(".png"));
    }

    @Test
    void testGif() {
        assertEquals("image/gif", map.getContentType(".gif"));
    }

    @Test
    void testSvg() {
        assertEquals("image/svg+xml", map.getContentType(".svg"));
    }

    @Test
    void testPdf() {
        assertEquals("application/pdf", map.getContentType(".pdf"));
    }

    @Test
    void testMp4() {
        assertEquals("video/mp4", map.getContentType(".mp4"));
    }

    @Test
    void testMp3() {
        assertEquals("audio/mpeg", map.getContentType(".mp3"));
    }

    @Test
    void testZip() {
        assertEquals("application/zip", map.getContentType(".zip"));
    }

    @Test
    void testJson() {
        assertEquals("application/json", map.getContentType(".json"));
    }

    @Test
    void testXml() {
        assertEquals("application/xml", map.getContentType(".xml"));
    }

    @Test
    void testTxt() {
        assertEquals("text/plain", map.getContentType(".txt"));
    }

    @Test
    void testDoc() {
        assertEquals("application/msword", map.getContentType(".doc"));
    }

    @Test
    void testDocx() {
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                map.getContentType(".docx"));
    }

    @Test
    void testXls() {
        assertEquals("application/vnd.ms-excel", map.getContentType(".xls"));
    }

    @Test
    void testPpt() {
        assertEquals("application/vnd.ms-powerpoint", map.getContentType(".ppt"));
    }

    @Test
    void testUnknownSuffixReturnsOctetStream() {
        assertEquals("application/octet-stream", map.getContentType(".unknown"));
        assertEquals("application/octet-stream", map.getContentType(".xyzabc"));
    }

    @Test
    void testEmptySuffix() {
        assertEquals("application/octet-stream", map.getContentType(""));
    }

    @Test
    void testSuffixWithoutDot() {
        assertEquals("application/octet-stream", map.getContentType("txt"));
    }

    /**
     * 映射表一致性校验：解析 ContentTypeMap 源码中声明的全部 case 分支，逐一验证
     * 每个后缀都能解析出对应的具体 ContentType，避免出现被错误映射为默认二进制流的
     * 死分支或漏分支。
     */
    @Test
    void testAllDeclaredSuffixesResolveToSpecificContentType() throws Exception {
        Path source = Paths.get("src/main/java/kohgylw/kiftd/server/util/ContentTypeMap.java");
        assertTrue(Files.exists(source), "未找到 ContentTypeMap 源码文件: " + source);
        List<String> suffixes = new ArrayList<>();
        for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("case \".") && trimmed.endsWith("\":")) {
                suffixes.add(trimmed.substring(6, trimmed.length() - 2));
            }
        }
        assertTrue(suffixes.size() > 100, "解析到的 case 分支数量异常: " + suffixes.size());
        for (String suffix : suffixes) {
            String type = map.getContentType(suffix);
            assertNotEquals("application/octet-stream", type, "后缀 " + suffix + " 不应被映射为默认类型");
        }
    }

}