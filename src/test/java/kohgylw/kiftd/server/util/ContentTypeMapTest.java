package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

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

}