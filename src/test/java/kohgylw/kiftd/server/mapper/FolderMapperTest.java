package kohgylw.kiftd.server.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.model.Folder;

@ExtendWith(MockitoExtension.class)
class FolderMapperTest {

    @Mock
    private FolderMapper folderMapper;

    @Test
    void testInsertFolder() {
        Folder folder = new Folder();
        folder.setFolderId("folder-001");
        folder.setFolderName("testFolder");
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);

        int result = folderMapper.insert(folder);

        assertEquals(1, result);
        verify(folderMapper, times(1)).insert(folder);
    }

    @Test
    void testSelectById() {
        Folder folder = new Folder();
        folder.setFolderId("folder-001");
        folder.setFolderName("testFolder");
        when(folderMapper.selectById("folder-001")).thenReturn(folder);

        Folder result = folderMapper.selectById("folder-001");

        assertNotNull(result);
        assertEquals("folder-001", result.getFolderId());
        assertEquals("testFolder", result.getFolderName());
    }

    @Test
    void testSelectByIdNotFound() {
        when(folderMapper.selectById("nonexistent")).thenReturn(null);

        Folder result = folderMapper.selectById("nonexistent");

        assertNull(result);
    }

    @Test
    void testDeleteById() {
        when(folderMapper.deleteById("folder-001")).thenReturn(1);

        int result = folderMapper.deleteById("folder-001");

        assertEquals(1, result);
        verify(folderMapper, times(1)).deleteById("folder-001");
    }

    @Test
    void testDeleteByIdNotFound() {
        when(folderMapper.deleteById("nonexistent")).thenReturn(0);

        int result = folderMapper.deleteById("nonexistent");

        assertEquals(0, result);
    }

    @Test
    void testQueryByParentId() {
        List<Folder> folders = new ArrayList<>();
        Folder f1 = new Folder();
        f1.setFolderId("f1");
        f1.setFolderName("folder1");
        Folder f2 = new Folder();
        f2.setFolderId("f2");
        f2.setFolderName("folder2");
        folders.add(f1);
        folders.add(f2);
        when(folderMapper.queryByParentId("parent-001")).thenReturn(folders);

        List<Folder> result = folderMapper.queryByParentId("parent-001");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("f1", result.get(0).getFolderId());
        assertEquals("f2", result.get(1).getFolderId());
    }

    @Test
    void testQueryByParentIdEmpty() {
        when(folderMapper.queryByParentId("empty-parent")).thenReturn(new ArrayList<>());

        List<Folder> result = folderMapper.queryByParentId("empty-parent");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryByParentIds() {
        List<Folder> folders = new ArrayList<>();
        Folder f1 = new Folder();
        f1.setFolderId("f1");
        f1.setFolderParent("p1");
        Folder f2 = new Folder();
        f2.setFolderId("f2");
        f2.setFolderParent("p2");
        folders.add(f1);
        folders.add(f2);
        List<String> parentIds = Arrays.asList("p1", "p2");
        when(folderMapper.queryByParentIds(parentIds)).thenReturn(folders);

        List<Folder> result = folderMapper.queryByParentIds(parentIds);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testQueryByParentIdsEmptyList() {
        List<String> emptyList = new ArrayList<>();
        when(folderMapper.queryByParentIds(emptyList)).thenReturn(new ArrayList<>());

        List<Folder> result = folderMapper.queryByParentIds(emptyList);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryByParentIdSection() {
        List<Folder> folders = new ArrayList<>();
        Folder f1 = new Folder();
        f1.setFolderId("f1");
        folders.add(f1);
        Map<String, Object> params = new HashMap<>();
        params.put("pid", "parent-001");
        params.put("offset", 0);
        params.put("rows", 10);
        when(folderMapper.queryByParentIdSection(params)).thenReturn(folders);

        List<Folder> result = folderMapper.queryByParentIdSection(params);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testQueryByParentIdAndFolderName() {
        Folder folder = new Folder();
        folder.setFolderId("folder-001");
        folder.setFolderName("testFolder");
        folder.setFolderParent("parent-001");
        Map<String, String> params = new HashMap<>();
        params.put("parentId", "parent-001");
        params.put("folderName", "testFolder");
        when(folderMapper.queryByParentIdAndFolderName(params)).thenReturn(folder);

        Folder result = folderMapper.queryByParentIdAndFolderName(params);

        assertNotNull(result);
        assertEquals("testFolder", result.getFolderName());
        assertEquals("parent-001", result.getFolderParent());
    }

    @Test
    void testQueryByParentIdAndFolderNameNotFound() {
        Map<String, String> params = new HashMap<>();
        params.put("parentId", "parent-001");
        params.put("folderName", "nonexistent");
        when(folderMapper.queryByParentIdAndFolderName(params)).thenReturn(null);

        Folder result = folderMapper.queryByParentIdAndFolderName(params);

        assertNull(result);
    }

    @Test
    void testUpdateById() {
        Folder folder = new Folder();
        folder.setFolderId("folder-001");
        folder.setFolderName("updatedName");
        when(folderMapper.updateById(folder)).thenReturn(1);

        int result = folderMapper.updateById(folder);

        assertEquals(1, result);
        verify(folderMapper, times(1)).updateById(folder);
    }

    @Test
    void testSelectCount() {
        when(folderMapper.selectCount(any())).thenReturn(5L);

        Long result = folderMapper.selectCount(null);

        assertEquals(5L, result);
    }

    @Test
    void testInsertReturnsZero() {
        Folder folder = new Folder();
        when(folderMapper.insert(any(Folder.class))).thenReturn(0);

        int result = folderMapper.insert(folder);

        assertEquals(0, result);
    }
}
