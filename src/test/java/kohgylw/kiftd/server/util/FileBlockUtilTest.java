package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.ExtendStores;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
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
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);

            String testBlockPath = "d:" + File.separator + "test" + File.separator + "fileblocks" + File.separator;
            when(mockReader.getFileBlockPath()).thenReturn(testBlockPath);

            File result = fileBlockUtil.getFileFromBlocks(nodeWithValidPath);
            assertNull(result, "When block file does not exist, should return null (no crash)");
        }
    }

    @Test
    void testGetFileFromBlocks_WithExtendStorePath_WhenStoreNotFound() {
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);

            when(mockReader.getExtendStores()).thenReturn(new ArrayList<>());

            File result = fileBlockUtil.getFileFromBlocks(nodeWithExtendStorePath);
            assertNull(result, "When extend store not found for index, should return null (no crash)");
        }
    }

    @Test
    void testGetFileFromBlocks_WithExtendStorePath_WhenStoreFoundButFileNotExist() {
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);

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
    void testGetFileFromBlocks_FilePathAfterNullCheck_ShouldNotReachConfigureReader() {
        File result = fileBlockUtil.getFileFromBlocks(nodeWithNullPath);
        assertNull(result);
    }

}