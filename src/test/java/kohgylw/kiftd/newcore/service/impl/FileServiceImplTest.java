package kohgylw.kiftd.newcore.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.TextFormateUtil;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileServiceImplTest {

    @Mock
    private FileNodeRepository fileNodeRepository;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private LogUtil logUtil;
    private Gson gson;
    @Mock
    private FileBlockUtil fileBlockUtil;
    @Mock
    private FolderUtil folderUtil;
    @Mock
    private IpAddrGetter ipAddrGetter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private MultipartFile multipartFile;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() throws Exception {
        gson = new Gson();
        fileService = new FileServiceImpl(fileNodeRepository, folderRepository, logUtil, gson, fileBlockUtil, folderUtil, ipAddrGetter);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testCheckUploadFile_NullFolderId() {
        when(request.getParameter("folderId")).thenReturn(null);

        String result = fileService.checkUploadFile(request, response);

        assertEquals("errorParameter", result);
    }

    @Test
    void testCheckUploadFile_EmptyFolderId() {
        when(request.getParameter("folderId")).thenReturn("");

        String result = fileService.checkUploadFile(request, response);

        assertEquals("errorParameter", result);
    }

    @Test
    void testCheckUploadFile_FolderNotFound() {
        when(request.getParameter("folderId")).thenReturn("folder1");
        when(folderRepository.selectById("folder1")).thenReturn(null);

        String result = fileService.checkUploadFile(request, response);

        assertEquals("errorParameter", result);
    }

    @Test
    void testCheckUploadFile_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);
            lenient().when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);

            String result = fileService.checkUploadFile(request, response);

            assertEquals("noAuthorized", result);
        }
    }

    @Test
    void testCheckUploadFile_InvalidMaxSize() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("namelist")).thenReturn("[\"test.txt\"]");
            when(request.getParameter("maxSize")).thenReturn("invalid");
            when(request.getParameter("maxFileIndex")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);

            String result = fileService.checkUploadFile(request, response);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testCheckUploadFile_FileTooLarge() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("namelist")).thenReturn("[\"largefile.txt\"]");
            when(request.getParameter("maxSize")).thenReturn("2048");
            when(request.getParameter("maxFileIndex")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.getUploadFileSize("user1")).thenReturn(1024L);
            when(textFormateUtil.matcherFileName(anyString())).thenReturn(true);

            String result = fileService.checkUploadFile(request, response);

            assertNotNull(result);
            assertTrue(result.contains("fileTooLarge"));
        }
    }

    @Test
    void testCheckUploadFile_InvalidFileName() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("namelist")).thenReturn("[\"invalid/name.txt\"]");
            when(request.getParameter("maxSize")).thenReturn("1024");
            when(request.getParameter("maxFileIndex")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.getUploadFileSize("user1")).thenReturn(-1L);
            when(textFormateUtil.matcherFileName("invalid/name.txt")).thenReturn(false);

            String result = fileService.checkUploadFile(request, response);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testCheckUploadFile_HasExistingNames() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            Node existingNode = new Node();
            existingNode.setFileName("existing.txt");
            List<Node> existingFiles = Collections.singletonList(existingNode);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("namelist")).thenReturn("[\"existing.txt\",\"newfile.txt\"]");
            when(request.getParameter("maxSize")).thenReturn("1024");
            when(request.getParameter("maxFileIndex")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.getUploadFileSize("user1")).thenReturn(-1L);
            when(textFormateUtil.matcherFileName(anyString())).thenReturn(true);
            when(fileNodeRepository.selectByParentFolderId("folder1")).thenReturn(existingFiles);
            when(fileNodeRepository.countByParentFolderId("folder1")).thenReturn(1L);

            String result = fileService.checkUploadFile(request, response);

            assertNotNull(result);
            assertTrue(result.contains("hasExistsNames"));
        }
    }

    @Test
    void testCheckUploadFile_PermitUpload() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("namelist")).thenReturn("[\"newfile.txt\"]");
            when(request.getParameter("maxSize")).thenReturn("1024");
            when(request.getParameter("maxFileIndex")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.getUploadFileSize("user1")).thenReturn(-1L);
            when(textFormateUtil.matcherFileName(anyString())).thenReturn(true);
            when(fileNodeRepository.selectByParentFolderId("folder1")).thenReturn(Collections.emptyList());
            when(fileNodeRepository.countByParentFolderId("folder1")).thenReturn(0L);

            String result = fileService.checkUploadFile(request, response);

            assertNotNull(result);
            assertTrue(result.contains("permitUpload"));
        }
    }

    @Test
    void testDoUploadFile_NullFolderId() {
        String result = fileService.doUploadFile(request, response, multipartFile);
        assertEquals("uploaderror", result);
    }

    @Test
    void testDoUploadFile_InvalidFileName() {
        try (MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(multipartFile.getOriginalFilename()).thenReturn("invalid/name.txt");
            when(textFormateUtil.matcherFileName("invalid/name.txt")).thenReturn(false);

            String result = fileService.doUploadFile(request, response, multipartFile);

            assertEquals("uploaderror", result);
        }
    }

    @Test
    void testDoUploadFile_FolderNotFound() {
        try (MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(textFormateUtil.matcherFileName("test.txt")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(null);

            String result = fileService.doUploadFile(request, response, multipartFile);

            assertEquals("uploaderror", result);
        }
    }

    @Test
    void testDoUploadFile_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(textFormateUtil.matcherFileName("test.txt")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);
            lenient().when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);

            String result = fileService.doUploadFile(request, response, multipartFile);

            assertEquals("uploaderror", result);
        }
    }

    @Test
    void testDoUploadFile_FileTooLarge() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(multipartFile.getSize()).thenReturn(2048L);
            when(textFormateUtil.matcherFileName("test.txt")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.getUploadFileSize("user1")).thenReturn(1024L);

            String result = fileService.doUploadFile(request, response, multipartFile);

            assertEquals("uploaderror", result);
        }
    }

    @Test
    void testDoUploadFile_RepeatSkip() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            Node existingNode = new Node();
            existingNode.setFileName("test.txt");
            List<Node> existingNodes = Collections.singletonList(existingNode);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("repeType")).thenReturn("skip");
            lenient().when(request.getParameter("fname")).thenReturn(null);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(textFormateUtil.matcherFileName("test.txt")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.getUploadFileSize("user1")).thenReturn(-1L);
            when(fileNodeRepository.selectByParentFolderId("folder1")).thenReturn(existingNodes);

            String result = fileService.doUploadFile(request, response, multipartFile);

            assertEquals("uploadsuccess", result);
        }
    }

    @Test
    void testDoUploadFile_RepeatNoType() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            Node existingNode = new Node();
            existingNode.setFileName("test.txt");
            List<Node> existingNodes = Collections.singletonList(existingNode);

            when(request.getParameter("folderId")).thenReturn("folder1");
            lenient().when(request.getParameter("repeType")).thenReturn(null);
            lenient().when(request.getParameter("fname")).thenReturn(null);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(textFormateUtil.matcherFileName("test.txt")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.getUploadFileSize("user1")).thenReturn(-1L);
            when(fileNodeRepository.selectByParentFolderId("folder1")).thenReturn(existingNodes);

            String result = fileService.doUploadFile(request, response, multipartFile);

            assertEquals("uploaderror", result);
        }
    }

    @Test
    void testDeleteFile_NullFileId() {
        String result = fileService.deleteFile(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testDeleteFile_EmptyFileId() {
        when(request.getParameter("fileId")).thenReturn("");
        String result = fileService.deleteFile(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testDeleteFile_NodeNotFound() {
        when(request.getParameter("fileId")).thenReturn("file1");
        when(fileNodeRepository.selectById("file1")).thenReturn(null);

        String result = fileService.deleteFile(request);

        assertEquals("deleteFileSuccess", result);
    }

    @Test
    void testDeleteFile_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);
            lenient().when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);

            String result = fileService.deleteFile(request);

            assertEquals("noAuthorized", result);
        }
    }

    @Test
    void testDeleteFile_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fileBlockUtil.deleteNode(node)).thenReturn(true);

            String result = fileService.deleteFile(request);

            assertEquals("deleteFileSuccess", result);
            verify(logUtil, times(1)).writeDeleteFileEvent(eq(request), eq(node));
        }
    }

    @Test
    void testDeleteFile_Failure() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fileBlockUtil.deleteNode(node)).thenReturn(false);

            String result = fileService.deleteFile(request);

            assertEquals("cannotDeleteFile", result);
        }
    }

    @Test
    void testDoDownloadFile_NullFileId() throws Exception {
        when(request.getParameter("fileId")).thenReturn(null);

        fileService.doDownloadFile(request, response);

        verify(response, times(1)).sendError(404);
    }

    @Test
    void testDoDownloadFile_NodeNotFound() throws Exception {
        when(request.getParameter("fileId")).thenReturn("file1");
        when(fileNodeRepository.selectById("file1")).thenReturn(null);

        fileService.doDownloadFile(request, response);

        verify(response, times(1)).sendError(404);
    }

    @Test
    void testDoDownloadFile_NoAuthorized() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(request.getParameter("fileId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            fileService.doDownloadFile(request, response);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testDoRenameFile_NullFileId() {
        String result = fileService.doRenameFile(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testDoRenameFile_NullNewFileName() {
        when(request.getParameter("fileId")).thenReturn("file1");
        String result = fileService.doRenameFile(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testDoRenameFile_InvalidNewFileName() {
        try (MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("fileId")).thenReturn("file1");
            when(request.getParameter("newFileName")).thenReturn("invalid/name.txt");
            when(textFormateUtil.matcherFileName("invalid/name.txt")).thenReturn(false);

            String result = fileService.doRenameFile(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testDoRenameFile_FileNotFound() {
        try (MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("fileId")).thenReturn("file1");
            when(request.getParameter("newFileName")).thenReturn("newname.txt");
            when(textFormateUtil.matcherFileName("newname.txt")).thenReturn(true);
            when(fileNodeRepository.selectById("file1")).thenReturn(null);

            String result = fileService.doRenameFile(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testDoRenameFile_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileName("oldname.txt");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("file1");
            when(request.getParameter("newFileName")).thenReturn("newname.txt");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(textFormateUtil.matcherFileName("newname.txt")).thenReturn(true);
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(false);

            String result = fileService.doRenameFile(request);

            assertEquals("noAuthorized", result);
        }
    }

    @Test
    void testDoRenameFile_SameName() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileName("samename.txt");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("file1");
            when(request.getParameter("newFileName")).thenReturn("samename.txt");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(ipAddrGetter.getIpAddr(request)).thenReturn("127.0.0.1");
            when(textFormateUtil.matcherFileName("samename.txt")).thenReturn(true);
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);

            String result = fileService.doRenameFile(request);

            assertEquals("renameFileSuccess", result);
        }
    }

    @Test
    void testDoRenameFile_NameOccupied() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileName("oldname.txt");
            node.setFileParentFolder("folder1");
            Node occupiedNode = new Node();
            occupiedNode.setFileName("newname.txt");
            List<Node> sameFolderNodes = Collections.singletonList(occupiedNode);
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("file1");
            when(request.getParameter("newFileName")).thenReturn("newname.txt");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(textFormateUtil.matcherFileName("newname.txt")).thenReturn(true);
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(fileNodeRepository.selectBySomeFolder("file1")).thenReturn(sameFolderNodes);

            String result = fileService.doRenameFile(request);

            assertEquals("nameOccupied", result);
        }
    }

    @Test
    void testDeleteCheckedFiles_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("strIdList")).thenReturn("[\"file1\"]");
            when(request.getParameter("strFidList")).thenReturn("[]");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(fileBlockUtil.deleteNode(node)).thenReturn(true);

            String result = fileService.deleteCheckedFiles(request);

            assertEquals("deleteFileSuccess", result);
        }
    }

    @Test
    void testDeleteCheckedFiles_Exception() {
        when(request.getParameter("strIdList")).thenReturn("invalid json");

        String result = fileService.deleteCheckedFiles(request);

        assertEquals("errorParameter", result);
    }

    @Test
    void testDownloadCheckedFiles_ZipDisabled() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isEnableDownloadByZip()).thenReturn(false);

            String result = fileService.downloadCheckedFiles(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testDownloadCheckedFiles_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            when(reader.isEnableDownloadByZip()).thenReturn(true);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(request.getParameter("strIdList")).thenReturn("[\"file1\"]");
            when(request.getParameter("strFidList")).thenReturn("[]");
            when(fileBlockUtil.createZip(anyList(), anyList(), eq("user1"))).thenReturn("test.zip");

            String result = fileService.downloadCheckedFiles(request);

            assertEquals("test.zip", result);
        }
    }

    @Test
    void testDownloadCheckedFiles_EmptyLists() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            when(reader.isEnableDownloadByZip()).thenReturn(true);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(request.getParameter("strIdList")).thenReturn("[]");
            when(request.getParameter("strFidList")).thenReturn("[]");

            String result = fileService.downloadCheckedFiles(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetPackTime_ZipDisabled() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isEnableDownloadByZip()).thenReturn(false);

            String result = fileService.getPackTime(request);

            assertEquals("0", result);
        }
    }

    @Test
    void testGetPackTime_Exception() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isEnableDownloadByZip()).thenReturn(true);
            when(request.getParameter("strIdList")).thenReturn("invalid json");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");

            String result = fileService.getPackTime(request);

            assertEquals("0", result);
        }
    }

    @Test
    void testConfirmMoveFiles_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("targetFolder");
            when(request.getParameter("locationpath")).thenReturn("targetFolder");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("targetFolder")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(false);

            String result = fileService.confirmMoveFiles(request);

            assertEquals("noAuthorized", result);
        }
    }

    @Test
    void testConfirmMoveFiles_Exception() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("targetFolder");
            when(request.getParameter("locationpath")).thenReturn("targetFolder");
            lenient().when(request.getParameter("method")).thenReturn("MOVE");
            lenient().when(request.getParameter("strIdList")).thenReturn("invalid json");
            lenient().when(request.getParameter("strFidList")).thenReturn("[]");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("targetFolder")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("targetFolder")).thenReturn(Collections.singletonList("targetFolder"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);

            String result = fileService.confirmMoveFiles(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testCheckImportFolder_NullFolderName() {
        String result = fileService.checkImportFolder(request);
        assertNotNull(result);
        assertTrue(result.contains("errorParameter"));
    }

    @Test
    void testCheckImportFolder_NullFolderId() {
        when(request.getParameter("folderName")).thenReturn("testfolder");
        String result = fileService.checkImportFolder(request);
        assertNotNull(result);
        assertTrue(result.contains("errorParameter"));
    }

    @Test
    void testCheckImportFolder_FolderNotFound() {
        when(request.getParameter("folderName")).thenReturn("testfolder");
        when(request.getParameter("folderId")).thenReturn("folder1");
        when(folderRepository.selectById("folder1")).thenReturn(null);

        String result = fileService.checkImportFolder(request);

        assertNotNull(result);
        assertTrue(result.contains("errorParameter"));
    }

    @Test
    void testDoImportFolder_NullFolderId() {
        String result = fileService.doImportFolder(request, multipartFile);
        assertEquals("uploaderror", result);
    }

    @Test
    void testDoImportFolder_FolderNotFound() {
        when(request.getParameter("folderId")).thenReturn("folder1");
        when(request.getParameter("originalFileName")).thenReturn("test/file.txt");
        when(folderRepository.selectById("folder1")).thenReturn(null);

        String result = fileService.doImportFolder(request, multipartFile);

        assertEquals("uploaderror", result);
    }
}
