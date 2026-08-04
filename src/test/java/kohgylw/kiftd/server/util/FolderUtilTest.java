package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Folder;

@ExtendWith(MockitoExtension.class)
class FolderUtilTest {

    @Mock
    private FolderMapper fm;

    @Mock
    private NodeMapper fim;

    @Mock
    private FileBlockUtil fbu;

    @InjectMocks
    private FolderUtil folderUtil;

    private Folder rootFolder;
    private Folder parentFolder;
    private Folder childFolder;

    @BeforeEach
    void setUp() {
        rootFolder = new Folder();
        rootFolder.setFolderId("root-id");
        rootFolder.setFolderName("root");
        rootFolder.setFolderParent("null");

        parentFolder = new Folder();
        parentFolder.setFolderId("parent-id");
        parentFolder.setFolderName("parent");
        parentFolder.setFolderParent("root-id");

        childFolder = new Folder();
        childFolder.setFolderId("child-id");
        childFolder.setFolderName("child");
        childFolder.setFolderParent("parent-id");
    }

    @Test
    void testGetParentListFromChild() {
        when(fm.selectById("child-id")).thenReturn(childFolder);
        when(fm.selectById("parent-id")).thenReturn(parentFolder);
        when(fm.selectById("root-id")).thenReturn(rootFolder);

        List<Folder> parents = folderUtil.getParentList("child-id");

        assertEquals(2, parents.size());
        assertEquals("root-id", parents.get(0).getFolderId());
        assertEquals("parent-id", parents.get(1).getFolderId());
    }

    @Test
    void testGetParentListRootFolder() {
        when(fm.selectById("root-id")).thenReturn(rootFolder);

        List<Folder> parents = folderUtil.getParentList("root-id");

        assertTrue(parents.isEmpty());
    }

    @Test
    void testGetParentListNonExistentFolder() {
        when(fm.selectById("nonexistent")).thenReturn(null);

        List<Folder> parents = folderUtil.getParentList("nonexistent");

        assertTrue(parents.isEmpty());
    }

    @Test
    void testGetParentListCircularReference() {
        Folder circular = new Folder();
        circular.setFolderId("circular-id");
        circular.setFolderParent("circular-id");

        when(fm.selectById("circular-id")).thenReturn(circular);

        List<Folder> parents = folderUtil.getParentList("circular-id");
        assertTrue(parents.isEmpty());
    }

    @Test
    void testGetAllFoldersIdFromRoot() {
        when(fm.selectById("root-id")).thenReturn(rootFolder);

        List<String> ids = folderUtil.getAllFoldersId("root-id");
        assertEquals(1, ids.size());
        assertEquals("root-id", ids.get(0));
    }

    @Test
    void testGetAllFoldersIdFromChild() {
        when(fm.selectById("child-id")).thenReturn(childFolder);
        when(fm.selectById("parent-id")).thenReturn(parentFolder);
        when(fm.selectById("root-id")).thenReturn(rootFolder);

        List<String> ids = folderUtil.getAllFoldersId("child-id");

        assertEquals(3, ids.size());
        assertEquals("root-id", ids.get(0));
        assertEquals("parent-id", ids.get(1));
        assertEquals("child-id", ids.get(2));
    }

}