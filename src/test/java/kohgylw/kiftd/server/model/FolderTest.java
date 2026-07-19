package kohgylw.kiftd.server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

class FolderTest {

    @Test
    void testFolderHasTableNameAnnotation() {
        TableName annotation = Folder.class.getAnnotation(TableName.class);
        assertNotNull(annotation);
        assertEquals("FOLDER", annotation.value());
    }

    @Test
    void testAllFieldsHaveAnnotations() throws Exception {
        java.lang.reflect.Field folderIdField = Folder.class.getDeclaredField("folderId");
        assertNotNull(folderIdField.getAnnotation(TableId.class));

        java.lang.reflect.Field folderNameField = Folder.class.getDeclaredField("folderName");
        assertNotNull(folderNameField.getAnnotation(TableField.class));

        java.lang.reflect.Field folderConstraintField = Folder.class.getDeclaredField("folderConstraint");
        assertNotNull(folderConstraintField.getAnnotation(TableField.class));
    }

    @Test
    void testNoTransientFields() {
        for (java.lang.reflect.Field field : Folder.class.getDeclaredFields()) {
            assertFalse(Modifier.isTransient(field.getModifiers()),
                    "Field " + field.getName() + " must not be transient");
        }
    }

    @Test
    void testGetAndSetAllFields() {
        Folder folder = new Folder();
        String id = "folder-001";
        String name = "test-folder";
        String date = "2026-01-15";
        String creator = "admin";
        String parent = "parent-001";
        int constraint = 3;

        folder.setFolderId(id);
        folder.setFolderName(name);
        folder.setFolderCreationDate(date);
        folder.setFolderCreator(creator);
        folder.setFolderParent(parent);
        folder.setFolderConstraint(constraint);

        assertEquals(id, folder.getFolderId());
        assertEquals(name, folder.getFolderName());
        assertEquals(date, folder.getFolderCreationDate());
        assertEquals(creator, folder.getFolderCreator());
        assertEquals(parent, folder.getFolderParent());
        assertEquals(constraint, folder.getFolderConstraint());
    }

    @Test
    void testDefaultFolderConstraintIsZero() {
        Folder folder = new Folder();
        assertEquals(0, folder.getFolderConstraint());
    }

    @Test
    void testFolderIdIsPrimaryKey() throws Exception {
        java.lang.reflect.Field folderIdField = Folder.class.getDeclaredField("folderId");
        TableId tableId = folderIdField.getAnnotation(TableId.class);
        assertEquals("folder_id", tableId.value());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : Folder.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(6, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : Folder.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testDefaultValuesAreNull() {
        Folder folder = new Folder();
        assertNull(folder.getFolderId());
        assertNull(folder.getFolderName());
        assertNull(folder.getFolderCreationDate());
        assertNull(folder.getFolderCreator());
        assertNull(folder.getFolderParent());
    }

    @Test
    void testSetNullValues() {
        Folder folder = new Folder();
        folder.setFolderId(null);
        folder.setFolderName(null);
        folder.setFolderCreationDate(null);
        folder.setFolderCreator(null);
        folder.setFolderParent(null);

        assertNull(folder.getFolderId());
        assertNull(folder.getFolderName());
        assertNull(folder.getFolderCreationDate());
        assertNull(folder.getFolderCreator());
        assertNull(folder.getFolderParent());
    }

    @Test
    void testEmptyStringValues() {
        Folder folder = new Folder();
        folder.setFolderId("");
        folder.setFolderName("");
        folder.setFolderCreationDate("");
        folder.setFolderCreator("");
        folder.setFolderParent("");

        assertEquals("", folder.getFolderId());
        assertEquals("", folder.getFolderName());
        assertEquals("", folder.getFolderCreationDate());
        assertEquals("", folder.getFolderCreator());
        assertEquals("", folder.getFolderParent());
    }

    @Test
    void testFolderConstraintBoundaryValues() {
        Folder folder = new Folder();

        folder.setFolderConstraint(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, folder.getFolderConstraint());

        folder.setFolderConstraint(-1);
        assertEquals(-1, folder.getFolderConstraint());

        folder.setFolderConstraint(0);
        assertEquals(0, folder.getFolderConstraint());

        folder.setFolderConstraint(1);
        assertEquals(1, folder.getFolderConstraint());

        folder.setFolderConstraint(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, folder.getFolderConstraint());
    }

    @Test
    void testLongStringValues() {
        Folder folder = new Folder();
        String longString = "a".repeat(10000);

        folder.setFolderId(longString);
        folder.setFolderName(longString);
        folder.setFolderCreationDate(longString);
        folder.setFolderCreator(longString);
        folder.setFolderParent(longString);

        assertEquals(longString, folder.getFolderId());
        assertEquals(longString, folder.getFolderName());
        assertEquals(longString, folder.getFolderCreationDate());
        assertEquals(longString, folder.getFolderCreator());
        assertEquals(longString, folder.getFolderParent());
    }

    @Test
    void testSpecialCharacterValues() {
        Folder folder = new Folder();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?\\n\\t";

        folder.setFolderId(specialChars);
        folder.setFolderName(specialChars);
        folder.setFolderCreationDate(specialChars);
        folder.setFolderCreator(specialChars);
        folder.setFolderParent(specialChars);

        assertEquals(specialChars, folder.getFolderId());
        assertEquals(specialChars, folder.getFolderName());
        assertEquals(specialChars, folder.getFolderCreationDate());
        assertEquals(specialChars, folder.getFolderCreator());
        assertEquals(specialChars, folder.getFolderParent());
    }

    @Test
    void testUnicodeValues() {
        Folder folder = new Folder();
        String unicode = "中文测试_日本語テスト_한국어테스트_😀🎉";

        folder.setFolderId(unicode);
        folder.setFolderName(unicode);
        folder.setFolderCreationDate(unicode);
        folder.setFolderCreator(unicode);
        folder.setFolderParent(unicode);

        assertEquals(unicode, folder.getFolderId());
        assertEquals(unicode, folder.getFolderName());
        assertEquals(unicode, folder.getFolderCreationDate());
        assertEquals(unicode, folder.getFolderCreator());
        assertEquals(unicode, folder.getFolderParent());
    }

    @Test
    void testMultipleSetAndGet() {
        Folder folder = new Folder();

        for (int i = 0; i < 10; i++) {
            folder.setFolderId("id-" + i);
            folder.setFolderName("name-" + i);
            folder.setFolderConstraint(i);
            assertEquals("id-" + i, folder.getFolderId());
            assertEquals("name-" + i, folder.getFolderName());
            assertEquals(i, folder.getFolderConstraint());
        }
    }

    @Test
    void testAllFieldsHaveFieldStrategyAnnotation() throws Exception {
        String[] stringFields = {"folderName", "folderCreationDate", "folderCreator", "folderParent", "folderConstraint"};
        for (String fieldName : stringFields) {
            java.lang.reflect.Field field = Folder.class.getDeclaredField(fieldName);
            TableField tableField = field.getAnnotation(TableField.class);
            assertNotNull(tableField, "Field " + fieldName + " must have @TableField annotation");
        }
    }

}