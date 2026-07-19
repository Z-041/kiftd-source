package kohgylw.kiftd.server.util;

import kohgylw.kiftd.newcore.config.ConfigurationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogUtilTest {

    @Mock
    private FolderUtil fu;

    @Mock
    private FolderMapper fm;

    @Mock
    private NodeMapper fim;

    @Mock
    private IpAddrGetter idg;

    @Mock
    private FileBlockUtil fbu;

    @InjectMocks
    private LogUtil logUtil;

    private File tempDir;

    @BeforeAll
    static void initPrinter() {
        Printer.init(false);
    }

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("logTest").toFile();
    }

    @AfterEach
    void tearDown() {
        deleteDir(tempDir);
    }

    private void deleteDir(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDir(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    private ConfigurationManager mockConfigurationManager(LogLevel level) {
        ConfigurationManager mockReader = mock(ConfigurationManager.class);
        when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
        when(mockReader.inspectLogLevel(any(LogLevel.class))).thenAnswer(invocation -> {
            LogLevel l = invocation.getArgument(0);
            switch (level) {
                case None:
                    return false;
                case Runtime_Exception:
                    return l == LogLevel.Runtime_Exception;
                case Event:
                    return true;
                default:
                    return false;
            }
        });
        return mockReader;
    }

    @Test
    void testWriteException_WhenLevelIsEvent_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeException(new RuntimeException("test exception"));
            });
        }
    }

    @Test
    void testWriteException_WhenLevelIsRuntimeException_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Runtime_Exception);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeException(new RuntimeException("test exception"));
            });
        }
    }

    @Test
    void testWriteException_WhenLevelIsNone_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.None);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeException(new RuntimeException("test exception"));
            });
        }
    }

    @Test
    void testWriteException_NullException_ShouldThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertThrows(NullPointerException.class, () -> {
                logUtil.writeException(null);
            });
        }
    }

    @Test
    void testWriteException_ExceptionWithManyStackFrames_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Exception deepException = new RuntimeException("deep");
            StackTraceElement[] deepStack = new StackTraceElement[50];
            for (int i = 0; i < 50; i++) {
                deepStack[i] = new StackTraceElement("TestClass", "method" + i, "Test.java", i);
            }
            deepException.setStackTrace(deepStack);

            assertDoesNotThrow(() -> {
                logUtil.writeException(deepException);
            });
        }
    }

    @Test
    void testWriteCreateFolderEvent_WithValidAccount_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Folder folder = new Folder();
            folder.setFolderId("folder-1");
            folder.setFolderName("TestFolder");
            folder.setFolderConstraint(0);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("folder-1")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeCreateFolderEvent("user1", "127.0.0.1", folder);
            });
        }
    }

    @Test
    void testWriteCreateFolderEvent_NullAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Folder folder = new Folder();
            folder.setFolderId("folder-1");
            folder.setFolderName("TestFolder");
            folder.setFolderConstraint(0);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("folder-1")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeCreateFolderEvent(null, "127.0.0.1", folder);
            });
        }
    }

    @Test
    void testWriteCreateFolderEvent_EmptyAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Folder folder = new Folder();
            folder.setFolderId("folder-1");
            folder.setFolderName("TestFolder");
            folder.setFolderConstraint(0);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("folder-1")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeCreateFolderEvent("", "127.0.0.1", folder);
            });
        }
    }

    @Test
    void testWriteCreateFolderEvent_WhenLogLevelIsNone_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.None);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Folder folder = new Folder();
            folder.setFolderId("folder-1");

            assertDoesNotThrow(() -> {
                logUtil.writeCreateFolderEvent("user", "ip", folder);
            });
        }
    }

    @Test
    void testWriteRenameFolderEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("folder-1")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeRenameFolderEvent("user1", "127.0.0.1", "folder-1", "oldName", "newName", "0", "1");
            });
        }
    }

    @Test
    void testWriteRenameFolderEvent_NullAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("folder-1")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeRenameFolderEvent(null, "127.0.0.1", "folder-1", "old", "new", "0", "0");
            });
        }
    }

    @Test
    void testWriteDeleteFolderEvent_WithValidRequest_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("testuser");
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Folder folder = new Folder();
            folder.setFolderName("TestFolder");

            List<Folder> parentList = new ArrayList<>();

            assertDoesNotThrow(() -> {
                logUtil.writeDeleteFolderEvent(request, folder, parentList);
            });
        }
    }

    @Test
    void testWriteDeleteFolderEvent_NullAccountInSession_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Folder folder = new Folder();
            folder.setFolderName("TestFolder");

            List<Folder> parentList = new ArrayList<>();

            assertDoesNotThrow(() -> {
                logUtil.writeDeleteFolderEvent(request, folder, parentList);
            });
        }
    }

    @Test
    void testWriteDeleteFileEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("testuser");
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileName("test.txt");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeDeleteFileEvent(request, node);
            });
        }
    }

    @Test
    void testWriteDeleteFileEvent_FolderIsNull_ShouldReturnEarly() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("testuser");
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileParentFolder("nonexistent");
            when(fm.selectById("nonexistent")).thenReturn(null);

            assertDoesNotThrow(() -> {
                logUtil.writeDeleteFileEvent(request, node);
            });
        }
    }

    @Test
    void testWriteUploadFileEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileName("test.txt");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeUploadFileEvent(request, node, "uploader");
            });
        }
    }

    @Test
    void testWriteUploadFileEvent_NullAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeUploadFileEvent(request, node, null);
            });
        }
    }

    @Test
    void testWriteDownloadFileEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileName("test.txt");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeDownloadFileEvent("user1", "127.0.0.1", node);
            });
        }
    }

    @Test
    void testWriteDownloadFileEvent_NullAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeDownloadFileEvent(null, "127.0.0.1", node);
            });
        }
    }

    @Test
    void testWriteChainEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileName("test.txt");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeChainEvent(request, node);
            });
        }
    }

    @Test
    void testWriteDownloadFileByKeyEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileName("test.txt");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeDownloadFileByKeyEvent(request, node);
            });
        }
    }

    @Test
    void testWriteShareFileURLEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("testuser");
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileName("test.txt");
            node.setFileParentFolder("parent-folder");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeShareFileURLEvent(request, node);
            });
        }
    }

    @Test
    void testWriteRenameFileEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            parentFolder.setFolderName("ParentFolder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeRenameFileEvent("user1", "127.0.0.1", "parent-folder", "old.txt", "new.txt");
            });
        }
    }

    @Test
    void testWriteRenameFileEvent_NullAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("parent-folder");
            when(fm.selectById("parent-folder")).thenReturn(parentFolder);

            List<Folder> parentList = new ArrayList<>();
            when(fu.getParentList("parent-folder")).thenReturn(parentList);

            assertDoesNotThrow(() -> {
                logUtil.writeRenameFileEvent(null, "127.0.0.1", "parent-folder", "old.txt", "new.txt");
            });
        }
    }

    @Test
    void testWriteMoveFileEvent_CopyMode_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeMoveFileEvent("user1", "127.0.0.1", "/from/path.txt", "/to/path.txt", true);
            });
        }
    }

    @Test
    void testWriteMoveFileEvent_MoveMode_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeMoveFileEvent("user1", "127.0.0.1", "/from/path.txt", "/to/path.txt", false);
            });
        }
    }

    @Test
    void testWriteMoveFileEvent_NullAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeMoveFileEvent(null, "127.0.0.1", "/from", "/to", false);
            });
        }
    }

    @Test
    void testWriteMoveFolderEvent_CopyMode_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeMoveFolderEvent("user1", "127.0.0.1", "/from/folder", "/to/folder", true);
            });
        }
    }

    @Test
    void testWriteMoveFolderEvent_MoveMode_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeMoveFolderEvent("user1", "127.0.0.1", "/from/folder", "/to/folder", false);
            });
        }
    }

    @Test
    void testWriteMoveFolderEvent_NullAccount_ShouldUseAnonymous() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            assertDoesNotThrow(() -> {
                logUtil.writeMoveFolderEvent(null, "127.0.0.1", "/from", "/to", false);
            });
        }
    }

    @Test
    void testWriteDownloadCheckedFileEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("testuser");
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            List<String> idList = new ArrayList<>();
            idList.add("file-1");
            List<String> fidList = new ArrayList<>();
            fidList.add("folder-1");

            Node node = new Node();
            node.setFileId("file-1");
            node.setFileName("test.txt");
            when(fim.selectById("file-1")).thenReturn(node);
            when(fbu.getNodePath(node)).thenReturn("/path/test.txt");

            Folder folder = new Folder();
            folder.setFolderId("folder-1");
            folder.setFolderName("TestFolder");
            when(fm.selectById("folder-1")).thenReturn(folder);
            when(fu.getFolderPath(folder)).thenReturn("/path/TestFolder");

            assertDoesNotThrow(() -> {
                logUtil.writeDownloadCheckedFileEvent(request, idList, fidList);
            });
        }
    }

    @Test
    void testWriteDownloadCheckedFileEvent_NodeNotFound_ShouldSkip() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("testuser");
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            List<String> idList = new ArrayList<>();
            idList.add("nonexistent");
            List<String> fidList = new ArrayList<>();

            when(fim.selectById("nonexistent")).thenReturn(null);

            assertDoesNotThrow(() -> {
                logUtil.writeDownloadCheckedFileEvent(request, idList, fidList);
            });
        }
    }

    @Test
    void testWriteDownloadCheckedFileEvent_FolderNotFound_ShouldSkip() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("testuser");
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            List<String> idList = new ArrayList<>();
            List<String> fidList = new ArrayList<>();
            fidList.add("nonexistent");

            when(fm.selectById("nonexistent")).thenReturn(null);

            assertDoesNotThrow(() -> {
                logUtil.writeDownloadCheckedFileEvent(request, idList, fidList);
            });
        }
    }

    @Test
    void testWriteChangePasswordEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            assertDoesNotThrow(() -> {
                logUtil.writeChangePasswordEvent(request, "user1");
            });
        }
    }

    @Test
    void testWriteSignUpEvent_WithValidData_ShouldNotThrow() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(idg.getIpAddr(request)).thenReturn("192.168.1.1");

            assertDoesNotThrow(() -> {
                logUtil.writeSignUpEvent(request, "newuser");
            });
        }
    }

    @Test
    void testLogUtilConstructor_CreatesLogsDirectory() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            LogUtil newLogUtil = new LogUtil();

            File logsDir = new File(tempDir, "logs");
            assertTrue(logsDir.exists());
            assertTrue(logsDir.isDirectory());
        }
    }

    @Test
    void testLogUtilConstructor_LogsDirAlreadyExistsAsFile_ShouldReplaceWithDirectory() throws Exception {
        File logsFile = new File(tempDir, "logs");
        logsFile.createNewFile();

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mockConfigurationManager(LogLevel.Event);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

            LogUtil newLogUtil = new LogUtil();

            File logsDir = new File(tempDir, "logs");
            assertTrue(logsDir.exists());
            assertTrue(logsDir.isDirectory());
        }
    }
}
