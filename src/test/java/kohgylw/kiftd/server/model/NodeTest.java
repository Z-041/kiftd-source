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

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : Node.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(7, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : Node.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testDefaultValuesAreNull() {
        Node node = new Node();
        assertNull(node.getFileId());
        assertNull(node.getFileName());
        assertNull(node.getFileSize());
        assertNull(node.getFileParentFolder());
        assertNull(node.getFileCreationDate());
        assertNull(node.getFileCreator());
        assertNull(node.getFilePath());
    }

    @Test
    void testSetNullValues() {
        Node node = new Node();
        node.setFileId(null);
        node.setFileName(null);
        node.setFileSize(null);
        node.setFileParentFolder(null);
        node.setFileCreationDate(null);
        node.setFileCreator(null);
        node.setFilePath(null);

        assertNull(node.getFileId());
        assertNull(node.getFileName());
        assertNull(node.getFileSize());
        assertNull(node.getFileParentFolder());
        assertNull(node.getFileCreationDate());
        assertNull(node.getFileCreator());
        assertNull(node.getFilePath());
    }

    @Test
    void testEmptyStringValues() {
        Node node = new Node();
        node.setFileId("");
        node.setFileName("");
        node.setFileSize("");
        node.setFileParentFolder("");
        node.setFileCreationDate("");
        node.setFileCreator("");
        node.setFilePath("");

        assertEquals("", node.getFileId());
        assertEquals("", node.getFileName());
        assertEquals("", node.getFileSize());
        assertEquals("", node.getFileParentFolder());
        assertEquals("", node.getFileCreationDate());
        assertEquals("", node.getFileCreator());
        assertEquals("", node.getFilePath());
    }

    @Test
    void testLongStringValues() {
        Node node = new Node();
        String longString = "a".repeat(10000);

        node.setFileId(longString);
        node.setFileName(longString);
        node.setFileSize(longString);
        node.setFileParentFolder(longString);
        node.setFileCreationDate(longString);
        node.setFileCreator(longString);
        node.setFilePath(longString);

        assertEquals(longString, node.getFileId());
        assertEquals(longString, node.getFileName());
        assertEquals(longString, node.getFileSize());
        assertEquals(longString, node.getFileParentFolder());
        assertEquals(longString, node.getFileCreationDate());
        assertEquals(longString, node.getFileCreator());
        assertEquals(longString, node.getFilePath());
    }

    @Test
    void testSpecialCharacterValues() {
        Node node = new Node();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?\\n\\t";

        node.setFileId(specialChars);
        node.setFileName(specialChars);
        node.setFileSize(specialChars);
        node.setFileParentFolder(specialChars);
        node.setFileCreationDate(specialChars);
        node.setFileCreator(specialChars);
        node.setFilePath(specialChars);

        assertEquals(specialChars, node.getFileId());
        assertEquals(specialChars, node.getFileName());
        assertEquals(specialChars, node.getFileSize());
        assertEquals(specialChars, node.getFileParentFolder());
        assertEquals(specialChars, node.getFileCreationDate());
        assertEquals(specialChars, node.getFileCreator());
        assertEquals(specialChars, node.getFilePath());
    }

    @Test
    void testUnicodeValues() {
        Node node = new Node();
        String unicode = "中文测试_日本語テスト_한국어테스트_😀🎉";

        node.setFileId(unicode);
        node.setFileName(unicode);
        node.setFileSize(unicode);
        node.setFileParentFolder(unicode);
        node.setFileCreationDate(unicode);
        node.setFileCreator(unicode);
        node.setFilePath(unicode);

        assertEquals(unicode, node.getFileId());
        assertEquals(unicode, node.getFileName());
        assertEquals(unicode, node.getFileSize());
        assertEquals(unicode, node.getFileParentFolder());
        assertEquals(unicode, node.getFileCreationDate());
        assertEquals(unicode, node.getFileCreator());
        assertEquals(unicode, node.getFilePath());
    }

    @Test
    void testMultipleSetAndGet() {
        Node node = new Node();

        for (int i = 0; i < 10; i++) {
            node.setFileId("id-" + i);
            node.setFileName("name-" + i);
            node.setFileSize(String.valueOf(i * 1024));
            assertEquals("id-" + i, node.getFileId());
            assertEquals("name-" + i, node.getFileName());
            assertEquals(String.valueOf(i * 1024), node.getFileSize());
        }
    }

    @Test
    void testAllFieldsHaveTableFieldAnnotation() throws Exception {
        String[] fields = {"fileName", "fileSize", "fileParentFolder", "fileCreationDate", "fileCreator", "filePath"};
        for (String fieldName : fields) {
            java.lang.reflect.Field field = Node.class.getDeclaredField(fieldName);
            TableField tableField = field.getAnnotation(TableField.class);
            assertNotNull(tableField, "Field " + fieldName + " must have @TableField annotation");
        }
    }

    @Test
    void testNoTransientFields() {
        for (java.lang.reflect.Field field : Node.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertFalse(Modifier.isTransient(field.getModifiers()),
                        "Field " + field.getName() + " must not be transient");
            }
        }
    }

}