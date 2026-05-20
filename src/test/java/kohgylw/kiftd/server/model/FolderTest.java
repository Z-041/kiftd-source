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

}