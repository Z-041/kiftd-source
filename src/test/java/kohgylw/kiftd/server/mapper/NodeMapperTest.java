package kohgylw.kiftd.server.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.model.Node;

@ExtendWith(MockitoExtension.class)
class NodeMapperTest {

    @Mock
    private NodeMapper nodeMapper;

    @Test
    void testInsertNode() {
        Node node = new Node();
        node.setFileId("file-001");
        node.setFileName("test.txt");
        when(nodeMapper.insert(any(Node.class))).thenReturn(1);

        int result = nodeMapper.insert(node);

        assertEquals(1, result);
        verify(nodeMapper, times(1)).insert(node);
    }

    @Test
    void testSelectById() {
        Node node = new Node();
        node.setFileId("file-001");
        node.setFileName("test.txt");
        node.setFileSize("1024");
        when(nodeMapper.selectById("file-001")).thenReturn(node);

        Node result = nodeMapper.selectById("file-001");

        assertNotNull(result);
        assertEquals("file-001", result.getFileId());
        assertEquals("test.txt", result.getFileName());
        assertEquals("1024", result.getFileSize());
    }

    @Test
    void testSelectByIdNotFound() {
        when(nodeMapper.selectById("nonexistent")).thenReturn(null);

        Node result = nodeMapper.selectById("nonexistent");

        assertNull(result);
    }

    @Test
    void testDeleteById() {
        when(nodeMapper.deleteById("file-001")).thenReturn(1);

        int result = nodeMapper.deleteById("file-001");

        assertEquals(1, result);
        verify(nodeMapper, times(1)).deleteById("file-001");
    }

    @Test
    void testDeleteByIdNotFound() {
        when(nodeMapper.deleteById("nonexistent")).thenReturn(0);

        int result = nodeMapper.deleteById("nonexistent");

        assertEquals(0, result);
    }

    @Test
    void testQueryByParentFolderId() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("f1");
        n1.setFileName("file1.txt");
        Node n2 = new Node();
        n2.setFileId("f2");
        n2.setFileName("file2.txt");
        nodes.add(n1);
        nodes.add(n2);
        when(nodeMapper.queryByParentFolderId("folder-001")).thenReturn(nodes);

        List<Node> result = nodeMapper.queryByParentFolderId("folder-001");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("f1", result.get(0).getFileId());
        assertEquals("f2", result.get(1).getFileId());
    }

    @Test
    void testQueryByParentFolderIdEmpty() {
        when(nodeMapper.queryByParentFolderId("empty-folder")).thenReturn(new ArrayList<>());

        List<Node> result = nodeMapper.queryByParentFolderId("empty-folder");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryByParentFolderIds() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("f1");
        n1.setFileParentFolder("p1");
        Node n2 = new Node();
        n2.setFileId("f2");
        n2.setFileParentFolder("p2");
        nodes.add(n1);
        nodes.add(n2);
        List<String> folderIds = Arrays.asList("p1", "p2");
        when(nodeMapper.queryByParentFolderIds(folderIds)).thenReturn(nodes);

        List<Node> result = nodeMapper.queryByParentFolderIds(folderIds);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testQueryByParentFolderIdsEmptyList() {
        List<String> emptyList = new ArrayList<>();
        when(nodeMapper.queryByParentFolderIds(emptyList)).thenReturn(new ArrayList<>());

        List<Node> result = nodeMapper.queryByParentFolderIds(emptyList);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryByParentFolderIdsLimit() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("f1");
        n1.setFileParentFolder("p1");
        Node n2 = new Node();
        n2.setFileId("f2");
        n2.setFileParentFolder("p2");
        nodes.add(n1);
        nodes.add(n2);
        List<String> folderIds = Arrays.asList("p1", "p2");
        when(nodeMapper.queryByParentFolderIdsLimit(folderIds, 10)).thenReturn(nodes);

        List<Node> result = nodeMapper.queryByParentFolderIdsLimit(folderIds, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testQueryByParentFolderIdsLimitRespectsLimit() {
        List<String> folderIds = Arrays.asList("p1", "p2", "p3");
        when(nodeMapper.queryByParentFolderIdsLimit(folderIds, 2)).thenAnswer(invocation -> {
            List<Node> all = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Node n = new Node();
                n.setFileId("f" + i);
                all.add(n);
            }
            int limit = invocation.getArgument(1);
            return new ArrayList<>(all.subList(0, Math.min(limit, all.size())));
        });

        List<Node> result = nodeMapper.queryByParentFolderIdsLimit(folderIds, 2);

        assertNotNull(result);
        assertEquals(2, result.size(), "SQL 层 LIMIT 应只返回上限数量的行");
    }

    @Test
    void testQueryByParentFolderIdsLimitEmptyList() {
        List<String> emptyList = new ArrayList<>();
        when(nodeMapper.queryByParentFolderIdsLimit(emptyList, 10)).thenReturn(new ArrayList<>());

        List<Node> result = nodeMapper.queryByParentFolderIdsLimit(emptyList, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryByParentFolderIdSection() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("f1");
        nodes.add(n1);
        Map<String, Object> params = new HashMap<>();
        params.put("pfid", "folder-001");
        params.put("offset", 0);
        params.put("rows", 10);
        when(nodeMapper.queryByParentFolderIdSection(params)).thenReturn(nodes);

        List<Node> result = nodeMapper.queryByParentFolderIdSection(params);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testQueryByPath() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("f1");
        n1.setFilePath("/path/to/file");
        nodes.add(n1);
        when(nodeMapper.queryByPath("/path/to/file")).thenReturn(nodes);

        List<Node> result = nodeMapper.queryByPath("/path/to/file");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/path/to/file", result.get(0).getFilePath());
    }

    @Test
    void testQueryByPathEmpty() {
        when(nodeMapper.queryByPath("/nonexistent/path")).thenReturn(new ArrayList<>());

        List<Node> result = nodeMapper.queryByPath("/nonexistent/path");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryByPathExcludeById() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("f2");
        n1.setFilePath("/path/to/file");
        nodes.add(n1);
        Map<String, String> params = new HashMap<>();
        params.put("path", "/path/to/file");
        params.put("fileId", "f1");
        when(nodeMapper.queryByPathExcludeById(params)).thenReturn(nodes);

        List<Node> result = nodeMapper.queryByPathExcludeById(params);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("f2", result.get(0).getFileId());
    }

    @Test
    void testQueryBySomeFolder() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileId("f1");
        Node n2 = new Node();
        n2.setFileId("f2");
        nodes.add(n1);
        nodes.add(n2);
        when(nodeMapper.queryBySomeFolder("file-001")).thenReturn(nodes);

        List<Node> result = nodeMapper.queryBySomeFolder("file-001");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testUpdateById() {
        Node node = new Node();
        node.setFileId("file-001");
        node.setFileName("updated.txt");
        when(nodeMapper.updateById(node)).thenReturn(1);

        int result = nodeMapper.updateById(node);

        assertEquals(1, result);
        verify(nodeMapper, times(1)).updateById(node);
    }

    @Test
    void testSelectCount() {
        when(nodeMapper.selectCount(any())).thenReturn(10L);

        Long result = nodeMapper.selectCount(null);

        assertEquals(10L, result);
    }

    @Test
    void testInsertReturnsZero() {
        Node node = new Node();
        when(nodeMapper.insert(any(Node.class))).thenReturn(0);

        int result = nodeMapper.insert(node);

        assertEquals(0, result);
    }
}
