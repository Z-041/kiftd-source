package kohgylw.kiftd.server.util;

import kohgylw.kiftd.newcore.config.ConfigurationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.ExtendStores;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileBlockUtilTest {

    @Mock
    private NodeMapper fm;

    @Mock
    private FolderMapper flm;

    @Mock
    private LogUtil lu;

    @Mock
    private FolderUtil fu;

    @InjectMocks
    private FileBlockUtil fileBlockUtil;

    private Node nodeWithNullPath;
    private Node nodeWithValidPath;
    private Node nodeWithExtendStorePath;

    @BeforeAll
    static void initPrinter() {
        Printer.init(false);
    }

    @BeforeEach
    void setUp() {
        nodeWithNullPath = new Node();
        nodeWithNullPath.setFileId("test-id-1");
        nodeWithNullPath.setFileName("test.txt");
        nodeWithNullPath.setFilePath(null);

        nodeWithValidPath = new Node();
        nodeWithValidPath.setFileId("test-id-2");
        nodeWithValidPath.setFileName("test2.txt");
        nodeWithValidPath.setFilePath("file_uuid.block");

        nodeWithExtendStorePath = new Node();
        nodeWithExtendStorePath.setFileId("test-id-3");
        nodeWithExtendStorePath.setFileName("test3.txt");
        nodeWithExtendStorePath.setFilePath("1_uuid.block");
    }

    @Test
    void testGetFileFromBlocks_WhenFilePathIsNull_ShouldReturnNull() {
        File result = fileBlockUtil.getFileFromBlocks(nodeWithNullPath);
        assertNull(result, "When filePath is null, getFileFromBlocks must return null without throwing NPE");
    }

    @Test
    void testGetFileFromBlocks_WhenNodeIsNull_ShouldReturnNull() {
        File result = fileBlockUtil.getFileFromBlocks(null);
        assertNull(result, "When node is null, getFileFromBlocks should return null (NPE caught by catch block)");
    }

    @Test
    void testGetFileFromBlocks_WithValidMainFilePath_WhenBlockExists() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            String testBlockPath = "d:" + File.separator + "test" + File.separator + "fileblocks" + File.separator;
            when(mockReader.getFileBlockPath()).thenReturn(testBlockPath);

            File result = fileBlockUtil.getFileFromBlocks(nodeWithValidPath);
            assertNull(result, "When block file does not exist, should return null (no crash)");
        }
    }

    @Test
    void testGetFileFromBlocks_WithExtendStorePath_WhenStoreNotFound() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            when(mockReader.getExtendStores()).thenReturn(new ArrayList<>());

            File result = fileBlockUtil.getFileFromBlocks(nodeWithExtendStorePath);
            assertNull(result, "When extend store not found for index, should return null (no crash)");
        }
    }

    @Test
    void testGetFileFromBlocks_WithExtendStorePath_WhenStoreFoundButFileNotExist() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            ExtendStores es = new ExtendStores();
            es.setIndex((short) 1);
            es.setPath(new File("d:" + File.separator + "nonexistent" + File.separator));
            List<ExtendStores> stores = new ArrayList<>();
            stores.add(es);
            when(mockReader.getExtendStores()).thenReturn(stores);

            File result = fileBlockUtil.getFileFromBlocks(nodeWithExtendStorePath);
            assertNull(result, "When block file does not exist in extend store, should return null (no crash)");
        }
    }

    @Test
	void testGetFileFromBlocks_FilePathAfterNullCheck_ShouldNotReachConfigurationManager() {
		File result = fileBlockUtil.getFileFromBlocks(nodeWithNullPath);
		assertNull(result);
	}

	@Test
	void testGetFileFromBlocks_WithPathTraversal_ShouldReturnNull() {
		Node node = new Node();
		node.setFileId("test-id-4");
		node.setFileName("test4.txt");
		node.setFilePath("file_../../etc/passwd.block");

		File result = fileBlockUtil.getFileFromBlocks(node);
		assertNull(result, "路径穿越格式的文件块索引应被拦截");
	}

    @Test
    void testGetFileSize_Zero() {
        String result = fileBlockUtil.getFileSize(0L);
        assertEquals("0", result);
    }

    @Test
    void testGetFileSize_Positive() {
        String result = fileBlockUtil.getFileSize(1024L);
        assertEquals("1024", result);
    }

    @Test
    void testGetFileSize_LargeValue() {
        String result = fileBlockUtil.getFileSize(1073741824L);
        assertEquals("1073741824", result);
    }

    @Test
    void testGetFileSize_Negative() {
        String result = fileBlockUtil.getFileSize(-1L);
        assertEquals("-1", result);
    }

    @Test
    void testGetETag_NullFile() {
        String result = fileBlockUtil.getETag(null);
        assertEquals("W\"0-0\"", result);
    }

    @Test
    void testGetETag_NonExistentFile() {
        File nonExistent = new File("nonexistent-file-12345.txt");
        String result = fileBlockUtil.getETag(nonExistent);
        assertEquals("W\"0-0\"", result);
    }

    @Test
    void testGetETag_ExistingFile() throws IOException {
        File tempFile = File.createTempFile("test-etag", ".block");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), "test content".getBytes());
        String result = fileBlockUtil.getETag(tempFile);
        assertNotNull(result);
        assertTrue(result.startsWith("W\""));
        assertTrue(result.endsWith("\""));
        assertTrue(result.contains("-"));
    }

    @Test
    void testGetETag_Directory() throws IOException {
        File tempDir = Files.createTempDirectory("test-etag-dir").toFile();
        tempDir.deleteOnExit();
        String result = fileBlockUtil.getETag(tempDir);
        assertNotNull(result);
        assertTrue(result.startsWith("W\""));
    }

    @Test
    void testDeleteNode_NullNode_ShouldReturnFalse() {
        boolean result = fileBlockUtil.deleteNode(null);
        assertFalse(result);
    }

    @Test
    void testDeleteNode_DeleteFails_ShouldReturnFalse() {
        Node node = new Node();
        node.setFileId("test-id");
        when(fm.deleteById("test-id")).thenReturn(0);
        boolean result = fileBlockUtil.deleteNode(node);
        assertFalse(result);
    }

    @Test
    void testDeleteNode_DeleteSucceeds_ShouldReturnTrue() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("file_test.block");
        when(fm.deleteById("test-id")).thenReturn(1);
        Map<String, String> map = new HashMap<>();
        map.put("path", "file_test.block");
        map.put("fileId", "test-id");
        List<Node> otherNodes = new ArrayList<>();
        when(fm.queryByPathExcludeById(any(Map.class))).thenReturn(otherNodes);
        boolean result = fileBlockUtil.deleteNode(node);
        assertTrue(result);
    }

    @Test
    void testIsValidNode_ValidNode_ShouldReturnTrue() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFileName("unique.txt");
        node.setFileParentFolder("parent-folder");
        Folder parentFolder = new Folder();
        parentFolder.setFolderId("parent-folder");
        when(flm.selectById("parent-folder")).thenReturn(parentFolder);
        List<Node> siblings = new ArrayList<>();
        Node otherNode = new Node();
        otherNode.setFileId("other-id");
        otherNode.setFileName("other.txt");
        siblings.add(otherNode);
        when(fm.queryByParentFolderId("parent-folder")).thenReturn(siblings);
        boolean result = fileBlockUtil.isValidNode(node);
        assertTrue(result);
    }

    @Test
    void testIsValidNode_NoParentFolder_ShouldReturnFalse() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFileParentFolder("nonexistent");
        when(flm.selectById("nonexistent")).thenReturn(null);
        boolean result = fileBlockUtil.isValidNode(node);
        assertFalse(result);
    }

    @Test
    void testIsValidNode_DuplicateName_ShouldReturnFalse() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFileName("duplicate.txt");
        node.setFileParentFolder("parent-folder");
        Folder parentFolder = new Folder();
        parentFolder.setFolderId("parent-folder");
        when(flm.selectById("parent-folder")).thenReturn(parentFolder);
        List<Node> siblings = new ArrayList<>();
        Node dupNode = new Node();
        dupNode.setFileId("dup-id");
        dupNode.setFileName("duplicate.txt");
        siblings.add(node);
        siblings.add(dupNode);
        when(fm.queryByParentFolderId("parent-folder")).thenReturn(siblings);
        boolean result = fileBlockUtil.isValidNode(node);
        assertFalse(result);
    }

    @Test
    void testGetFileFromBlocks_InvalidFileNameFormat_ShouldReturnNull() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("invalid-format");
        File result = fileBlockUtil.getFileFromBlocks(node);
        assertNull(result);
    }

    @Test
    void testGetFileFromBlocks_EmptyFilePath_ShouldReturnNull() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("");
        File result = fileBlockUtil.getFileFromBlocks(node);
        assertNull(result);
    }

    @Test
    void testGetFileFromBlocks_InvalidPrefix_ShouldReturnNull() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("abc_test.block");
        File result = fileBlockUtil.getFileFromBlocks(node);
        assertNull(result);
    }

    @Test
    void testGetFileFromBlocks_NoExtension_ShouldReturnNull() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("file_test");
        File result = fileBlockUtil.getFileFromBlocks(node);
        assertNull(result);
    }

    @Test
    void testGetFileFromBlocks_SpecialCharsInName_ShouldReturnNull() {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("file_../escape.block");
        File result = fileBlockUtil.getFileFromBlocks(node);
        assertNull(result);
    }

    @Test
    void testInitTempDir_DirectoryDoesNotExist_ShouldCreate() throws IOException {
        File tempParent = Files.createTempDirectory("test-init-temp").toFile();
        tempParent.deleteOnExit();
        File tempDir = new File(tempParent, "temporaryfiles");
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getTemporaryfilePath()).thenReturn(tempDir.getAbsolutePath());
            fileBlockUtil.initTempDir();
            assertTrue(tempDir.exists());
            assertTrue(tempDir.isDirectory());
        }
    }

    @Test
    void testInitTempDir_DirectoryExists_ShouldCleanNonHiddenFiles() throws IOException {
        File tempDir = Files.createTempDirectory("test-init-temp2").toFile();
        tempDir.deleteOnExit();
        File visibleFile = new File(tempDir, "visible.txt");
        visibleFile.createNewFile();
        File hiddenFile = new File(tempDir, ".hidden");
        hiddenFile.createNewFile();
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getTemporaryfilePath()).thenReturn(tempDir.getAbsolutePath());
            fileBlockUtil.initTempDir();
            assertFalse(visibleFile.exists());
            assertTrue(hiddenFile.exists());
        }
    }

    @Test
    void testGetFileFromBlocks_WithValidBlockFile_ShouldReturnFile() throws IOException {
        File tempBlockDir = Files.createTempDirectory("test-blocks").toFile();
        tempBlockDir.deleteOnExit();
        File blockFile = new File(tempBlockDir, "file_test123.block");
        blockFile.createNewFile();
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("file_test123.block");
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getFileBlockPath()).thenReturn(tempBlockDir.getAbsolutePath() + File.separator);
            File result = fileBlockUtil.getFileFromBlocks(node);
            assertNotNull(result);
            assertTrue(result.exists());
            assertEquals("file_test123.block", result.getName());
        }
    }

    @Test
    void testGetFileFromBlocks_ExtendStoreWithValidFile_ShouldReturnFile() throws IOException {
        File extendDir = Files.createTempDirectory("test-extend").toFile();
        extendDir.deleteOnExit();
        File blockFile = new File(extendDir, "2_test456.block");
        blockFile.createNewFile();
        Node node = new Node();
        node.setFileId("test-id");
        node.setFilePath("2_test456.block");
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            ExtendStores es = new ExtendStores();
            es.setIndex((short) 2);
            es.setPath(extendDir);
            List<ExtendStores> stores = new ArrayList<>();
            stores.add(es);
            when(mockReader.getExtendStores()).thenReturn(stores);
            File result = fileBlockUtil.getFileFromBlocks(node);
            assertNotNull(result);
            assertTrue(result.exists());
            assertEquals("2_test456.block", result.getName());
        }
    }

    @Test
    void testInsertNewNode_ValidInsertion_ShouldReturnNode() {
        when(fm.insert(any(Node.class))).thenReturn(1);
        Folder parentFolder = new Folder();
        parentFolder.setFolderId("parent-id");
        when(flm.selectById("parent-id")).thenReturn(parentFolder);
        List<Node> siblings = new ArrayList<>();
        when(fm.queryByParentFolderId("parent-id")).thenReturn(siblings);
        Node result = fileBlockUtil.insertNewNode("test.txt", "user1", "file_test.block", "1024", "parent-id");
        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        assertEquals("user1", result.getFileCreator());
    }

    @Test
    void testInsertNewNode_NullAccount_ShouldUseAnonymous() {
        when(fm.insert(any(Node.class))).thenReturn(1);
        Folder parentFolder = new Folder();
        parentFolder.setFolderId("parent-id");
        when(flm.selectById("parent-id")).thenReturn(parentFolder);
        List<Node> siblings = new ArrayList<>();
        when(fm.queryByParentFolderId("parent-id")).thenReturn(siblings);
        Node result = fileBlockUtil.insertNewNode("test.txt", null, "file_test.block", "1024", "parent-id");
        assertNotNull(result);
        assertEquals("匿名用户", result.getFileCreator());
    }

    @Test
    void testInsertNewNode_InsertFails_ShouldReturnNull() {
        when(fm.insert(any(Node.class))).thenReturn(0);
        Node result = fileBlockUtil.insertNewNode("test.txt", "user", "file_test.block", "1024", "parent-id");
        assertNull(result);
    }

    @Test
    void testGetNodePath_ValidNode_ShouldReturnPath() {
        Node node = new Node();
        node.setFileName("test.txt");
        node.setFileParentFolder("parent-id");
        Folder parentFolder = new Folder();
        parentFolder.setFolderId("parent-id");
        parentFolder.setFolderName("Parent");
        when(flm.selectById("parent-id")).thenReturn(parentFolder);
        List<Folder> parentList = new ArrayList<>();
        Folder rootFolder = new Folder();
        rootFolder.setFolderName("ROOT");
        parentList.add(rootFolder);
        when(fu.getParentList("parent-id")).thenReturn(parentList);
        String result = fileBlockUtil.getNodePath(node);
        assertNotNull(result);
        assertTrue(result.contains("test.txt"));
        assertTrue(result.contains("Parent"));
        assertTrue(result.contains("ROOT"));
    }
}