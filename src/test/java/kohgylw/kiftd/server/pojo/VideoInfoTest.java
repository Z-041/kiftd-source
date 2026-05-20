package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import kohgylw.kiftd.server.model.Node;

class VideoInfoTest {

    @Test
    void testConstructorCopiesAllNodeFields() {
        Node original = new Node();
        original.setFileId("video-001");
        original.setFileName("demo.mp4");
        original.setFileParentFolder("folder-001");
        original.setFilePath("file_demo-uuid.block");
        original.setFileSize("1048576");
        original.setFileCreationDate("2026-01-15");
        original.setFileCreator("admin");

        VideoInfo video = new VideoInfo(original);

        assertEquals(original.getFileId(), video.getFileId());
        assertEquals(original.getFileName(), video.getFileName());
        assertEquals(original.getFileParentFolder(), video.getFileParentFolder());
        assertEquals(original.getFilePath(), video.getFilePath());
        assertEquals(original.getFileSize(), video.getFileSize());
        assertEquals(original.getFileCreationDate(), video.getFileCreationDate());
        assertEquals(original.getFileCreator(), video.getFileCreator());
    }

    @Test
    void testConstructorWithNodeHavingNullFields() {
        Node original = new Node();
        VideoInfo video = new VideoInfo(original);

        assertNull(video.getFileId());
        assertNull(video.getFileName());
        assertNull(video.getFileParentFolder());
        assertNull(video.getFilePath());
        assertNull(video.getFileSize());
        assertNull(video.getFileCreationDate());
        assertNull(video.getFileCreator());
    }

    @Test
    void testSetAndGetNeedEncode() {
        Node original = new Node();
        original.setFileId("video-002");
        VideoInfo video = new VideoInfo(original);

        assertNull(video.getNeedEncode());

        video.setNeedEncode("Y");
        assertEquals("Y", video.getNeedEncode());

        video.setNeedEncode("N");
        assertEquals("N", video.getNeedEncode());
    }

    @Test
    void testVideoInfoIsInstanceOfNode() {
        Node original = new Node();
        VideoInfo video = new VideoInfo(original);
        assertTrue(video instanceof Node);
        assertTrue(video instanceof VideoInfo);
    }

    @Test
    void testCopiedFieldsAreIndependent() {
        Node original = new Node();
        original.setFileId("original-id");
        VideoInfo video = new VideoInfo(original);

        original.setFileId("modified-id");
        assertNotEquals(original.getFileId(), video.getFileId(),
                "VideoInfo should copy values, not hold references");
    }

}