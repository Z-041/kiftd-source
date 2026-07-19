package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;

class FolderViewTest {

    @Test
    void testDefaultValues() {
        FolderView folderView = new FolderView();
        assertNull(folderView.getFolder());
        assertNull(folderView.getParentList());
        assertNull(folderView.getFolderList());
        assertNull(folderView.getFileList());
        assertNull(folderView.getAccount());
        assertNull(folderView.getAuthList());
        assertNull(folderView.getPublishTime());
        assertNull(folderView.getAllowChangePassword());
        assertNull(folderView.getShowFileChain());
        assertNull(folderView.getAllowSignUp());
        assertFalse(folderView.isEnableDownloadZip());
        assertFalse(folderView.isEnableFFMPEG());
        assertEquals(0L, folderView.getFoldersOffset());
        assertEquals(0L, folderView.getFilesOffset());
        assertEquals(0, folderView.getSelectStep());
    }

    @Test
    void testSetAndGetAllStringFields() {
        FolderView folderView = new FolderView();
        String account = "user123";
        String publishTime = "2026-01-15";
        String allowChangePassword = "true";
        String showFileChain = "false";
        String allowSignUp = "true";

        folderView.setAccount(account);
        folderView.setPublishTime(publishTime);
        folderView.setAllowChangePassword(allowChangePassword);
        folderView.setShowFileChain(showFileChain);
        folderView.setAllowSignUp(allowSignUp);

        assertEquals(account, folderView.getAccount());
        assertEquals(publishTime, folderView.getPublishTime());
        assertEquals(allowChangePassword, folderView.getAllowChangePassword());
        assertEquals(showFileChain, folderView.getShowFileChain());
        assertEquals(allowSignUp, folderView.getAllowSignUp());
    }

    @Test
    void testSetAndGetFolder() {
        FolderView folderView = new FolderView();
        Folder folder = new Folder();
        folder.setFolderId("folder-001");
        folder.setFolderName("testFolder");

        folderView.setFolder(folder);

        assertNotNull(folderView.getFolder());
        assertEquals("folder-001", folderView.getFolder().getFolderId());
        assertEquals("testFolder", folderView.getFolder().getFolderName());
    }

    @Test
    void testSetAndGetParentList() {
        FolderView folderView = new FolderView();
        List<Folder> parentList = new ArrayList<>();
        Folder f1 = new Folder();
        f1.setFolderId("parent-001");
        Folder f2 = new Folder();
        f2.setFolderId("parent-002");
        parentList.add(f1);
        parentList.add(f2);

        folderView.setParentList(parentList);

        assertNotNull(folderView.getParentList());
        assertEquals(2, folderView.getParentList().size());
    }

    @Test
    void testSetAndGetFolderList() {
        FolderView folderView = new FolderView();
        List<Folder> folderList = new ArrayList<>();
        Folder f1 = new Folder();
        f1.setFolderId("f1");
        folderList.add(f1);

        folderView.setFolderList(folderList);

        assertNotNull(folderView.getFolderList());
        assertEquals(1, folderView.getFolderList().size());
    }

    @Test
    void testSetAndGetFileList() {
        FolderView folderView = new FolderView();
        List<Node> fileList = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("file-001");
        fileList.add(n1);

        folderView.setFileList(fileList);

        assertNotNull(folderView.getFileList());
        assertEquals(1, folderView.getFileList().size());
    }

    @Test
    void testSetAndGetAuthList() {
        FolderView folderView = new FolderView();
        List<String> authList = Arrays.asList("read", "write", "delete");

        folderView.setAuthList(authList);

        assertNotNull(folderView.getAuthList());
        assertEquals(3, folderView.getAuthList().size());
        assertTrue(folderView.getAuthList().contains("read"));
    }

    @Test
    void testSetAndGetBooleanFields() {
        FolderView folderView = new FolderView();

        folderView.setEnableDownloadZip(true);
        folderView.setEnableFFMPEG(true);
        assertTrue(folderView.isEnableDownloadZip());
        assertTrue(folderView.isEnableFFMPEG());

        folderView.setEnableDownloadZip(false);
        folderView.setEnableFFMPEG(false);
        assertFalse(folderView.isEnableDownloadZip());
        assertFalse(folderView.isEnableFFMPEG());
    }

    @Test
    void testSetAndGetLongFields() {
        FolderView folderView = new FolderView();

        folderView.setFoldersOffset(100L);
        folderView.setFilesOffset(200L);
        assertEquals(100L, folderView.getFoldersOffset());
        assertEquals(200L, folderView.getFilesOffset());
    }

    @Test
    void testSetAndGetSelectStep() {
        FolderView folderView = new FolderView();

        folderView.setSelectStep(50);
        assertEquals(50, folderView.getSelectStep());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : FolderView.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(15, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : FolderView.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testSetNullValues() {
        FolderView folderView = new FolderView();
        folderView.setAccount("test");
        folderView.setPublishTime("test");
        folderView.setFolder(new Folder());
        folderView.setParentList(new ArrayList<>());
        folderView.setFolderList(new ArrayList<>());
        folderView.setFileList(new ArrayList<>());
        folderView.setAuthList(new ArrayList<>());

        folderView.setAccount(null);
        folderView.setPublishTime(null);
        folderView.setFolder(null);
        folderView.setParentList(null);
        folderView.setFolderList(null);
        folderView.setFileList(null);
        folderView.setAuthList(null);

        assertNull(folderView.getAccount());
        assertNull(folderView.getPublishTime());
        assertNull(folderView.getFolder());
        assertNull(folderView.getParentList());
        assertNull(folderView.getFolderList());
        assertNull(folderView.getFileList());
        assertNull(folderView.getAuthList());
    }

    @Test
    void testLongBoundaryValues() {
        FolderView folderView = new FolderView();

        folderView.setFoldersOffset(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, folderView.getFoldersOffset());

        folderView.setFoldersOffset(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, folderView.getFoldersOffset());

        folderView.setFilesOffset(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, folderView.getFilesOffset());

        folderView.setFilesOffset(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, folderView.getFilesOffset());
    }

    @Test
    void testIntBoundaryValues() {
        FolderView folderView = new FolderView();

        folderView.setSelectStep(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, folderView.getSelectStep());

        folderView.setSelectStep(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, folderView.getSelectStep());
    }
}
