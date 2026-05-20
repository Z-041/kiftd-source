package kohgylw.kiftd.server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

class NodeTest {

    @Test
    void testNodeFieldsExist() throws Exception {
        Node node = new Node();
        assertNotNull(node);
    }

    @Test
    void testSetAndGetFilePath() {
        Node node = new Node();
        String testPath = "file_uuid.block";
        node.setFilePath(testPath);
        assertEquals(testPath, node.getFilePath());
    }

    @Test
    void testFilePathIsNotNullByDefault() {
        Node node = new Node();
        assertNull(node.getFilePath());
    }

    @Test
    void testFilePathFieldIsNotTransient() throws Exception {
        java.lang.reflect.Field filePathField = Node.class.getDeclaredField("filePath");
        int modifiers = filePathField.getModifiers();
        assertFalse(Modifier.isTransient(modifiers),
                "filePath field must NOT be transient to allow MyBatis Plus to map it from database");
    }

    @Test
    void testFilePathHasTableFieldAnnotation() throws Exception {
        java.lang.reflect.Field filePathField = Node.class.getDeclaredField("filePath");
        TableField annotation = filePathField.getAnnotation(TableField.class);
        assertNotNull(annotation, "filePath field must have @TableField annotation");
    }

    @Test
    void testFileIdHasTableIdAnnotation() throws Exception {
        java.lang.reflect.Field fileIdField = Node.class.getDeclaredField("fileId");
        TableId annotation = fileIdField.getAnnotation(TableId.class);
        assertNotNull(annotation, "fileId field must have @TableId annotation");
    }

    @Test
    void testNodeHasTableNameAnnotation() {
        TableName annotation = Node.class.getAnnotation(TableName.class);
        assertNotNull(annotation, "Node class must have @TableName annotation");
        assertEquals("FILE", annotation.value());
    }

    @Test
    void testAllFieldsAreAccessibleViaGettersAndSetters() {
        Node node = new Node();
        String testId = "test-id";
        String testName = "test.txt";
        String testSize = "1024";
        String testParent = "parent-id";
        String testDate = "2026-01-01";
        String testCreator = "admin";
        String testPath = "file_test.block";

        node.setFileId(testId);
        node.setFileName(testName);
        node.setFileSize(testSize);
        node.setFileParentFolder(testParent);
        node.setFileCreationDate(testDate);
        node.setFileCreator(testCreator);
        node.setFilePath(testPath);

        assertEquals(testId, node.getFileId());
        assertEquals(testName, node.getFileName());
        assertEquals(testSize, node.getFileSize());
        assertEquals(testParent, node.getFileParentFolder());
        assertEquals(testDate, node.getFileCreationDate());
        assertEquals(testCreator, node.getFileCreator());
        assertEquals(testPath, node.getFilePath());
    }

}