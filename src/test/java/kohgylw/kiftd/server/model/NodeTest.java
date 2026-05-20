package kohgylw.kiftd.server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

class NodeTest {

    @Test
    void testNodeIsMappedToFILE_Table() {
        TableName annotation = Node.class.getAnnotation(TableName.class);
        assertNotNull(annotation, "Node must be mapped to a database table via @TableName");
        assertEquals("FILE", annotation.value(), "Node must map to FILE table");
    }

    @Test
    void testFileIdIsPrimaryKey() throws Exception {
        java.lang.reflect.Field fileIdField = Node.class.getDeclaredField("fileId");
        TableId annotation = fileIdField.getAnnotation(TableId.class);
        assertNotNull(annotation, "fileId must be @TableId for MyBatis Plus primary key mapping");
    }

    @Test
    void testFilePathIsPersistable() throws Exception {
        java.lang.reflect.Field filePathField = Node.class.getDeclaredField("filePath");
        int modifiers = filePathField.getModifiers();
        assertFalse(Modifier.isTransient(modifiers),
                "filePath must NOT be transient - transient causes NullPointerException in getFilePath() after DB read");
        TableField annotation = filePathField.getAnnotation(TableField.class);
        assertNotNull(annotation, "filePath must have @TableField for column mapping");
    }

    @Test
    void testFullDataRoundTripMatchesDatabaseColumns() {
        Node node = new Node();
        node.setFileId("ff123456-7890-4abc-def0-123456789abc");
        node.setFileName("report.pdf");
        node.setFileSize("2048576");
        node.setFileParentFolder("root");
        node.setFileCreationDate("2026-05-20 10:30:00");
        node.setFileCreator("admin");
        node.setFilePath("STORAGE/ff12/ff123456-7890-4abc-def0-123456789abc.block");

        assertEquals("ff123456-7890-4abc-def0-123456789abc", node.getFileId());
        assertEquals("report.pdf", node.getFileName());
        assertEquals("2048576", node.getFileSize());
        assertEquals("root", node.getFileParentFolder());
        assertEquals("2026-05-20 10:30:00", node.getFileCreationDate());
        assertEquals("admin", node.getFileCreator());
        assertEquals("STORAGE/ff12/ff123456-7890-4abc-def0-123456789abc.block", node.getFilePath());
    }

}