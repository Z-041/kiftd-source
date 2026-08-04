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

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.PictureViewList;
import kohgylw.kiftd.server.pojo.VideoInfo;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.printer.Printer;

import java.io.File;
import java.util.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MediaServiceImplTest {

    @Mock
    private FileNodeRepository fileNodeRepository;
    @Mock
    private FolderRepository folderRepository;
    private Gson gson;
    @Mock
    private FileBlockUtil fileBlockUtil;
    @Mock
    private FolderUtil folderUtil;
    @Mock
    private LogUtil logUtil;
    @Mock
    private KiftdFFMPEGLocator kiftdFFMPEGLocator;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private Printer printer;

    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() throws Exception {
        gson = new Gson();
        Printer.instance = printer;
        mediaService = new MediaServiceImpl(fileNodeRepository, folderRepository, gson, fileBlockUtil, folderUtil, logUtil, kiftdFFMPEGLocator);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    // ==================== 图片相关测试 ====================

    @Test
    void testGetPreviewPictureJson_NullFileId() {
        when(request.getParameter("fileId")).thenReturn(null);
        String result = mediaService.getPreviewPictureJson(request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetPreviewPictureJson_EmptyFileId() {
        when(request.getParameter("fileId")).thenReturn("");
        String result = mediaService.getPreviewPictureJson(request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetPreviewPictureJson_FileNotFound() {
        when(request.getParameter("fileId")).thenReturn("pic1");
        when(fileNodeRepository.selectById("pic1")).thenReturn(null);

        String result = mediaService.getPreviewPictureJson(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetPreviewPictureJson_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("pic1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("pic1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("pic1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = mediaService.getPreviewPictureJson(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetPreviewPictureJson_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node picNode = new Node();
            picNode.setFileId("pic1");
            picNode.setFileName("photo.jpg");
            picNode.setFileParentFolder("folder1");
            picNode.setFileSize("102400");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            List<Node> sameFolderNodes = Collections.singletonList(picNode);
            File mockFile = mock(File.class);

            when(request.getParameter("fileId")).thenReturn("pic1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("pic1")).thenReturn(picNode);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fileNodeRepository.selectBySomeFolder("pic1")).thenReturn(sameFolderNodes);
            when(fileBlockUtil.getFileFromBlocks(any(Node.class))).thenReturn(mockFile);
            when(mockFile.lastModified()).thenReturn(1234567890L);

            String result = mediaService.getPreviewPictureJson(request);

            assertNotNull(result);
            assertTrue(result.contains("photo.jpg"));
        }
    }

    @Test
    void testGetCondensedPicture_NullFileId() throws Exception {
        when(request.getParameter("fileId")).thenReturn(null);
        mediaService.getCondensedPicture(request, response);
        verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void testGetCondensedPicture_FileNotFound() throws Exception {
        when(request.getParameter("fileId")).thenReturn("pic1");
        when(fileNodeRepository.selectById("pic1")).thenReturn(null);

        mediaService.getCondensedPicture(request, response);

        verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void testGetCondensedPicture_NoAuthorized() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("pic1");
            node.setFileParentFolder("folder1");

            when(request.getParameter("fileId")).thenReturn("pic1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("pic1")).thenReturn(node);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            mediaService.getCondensedPicture(request, response);

            verify(response, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    // ==================== 视频相关测试 ====================

    @Test
    void testGetPlayVideoJson_NullFileId() {
        when(request.getParameter("fileId")).thenReturn(null);
        String result = mediaService.getPlayVideoJson(request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetPlayVideoJson_EmptyFileId() {
        when(request.getParameter("fileId")).thenReturn("");
        String result = mediaService.getPlayVideoJson(request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetPlayVideoJson_FileNotFound() {
        when(request.getParameter("fileId")).thenReturn("video1");
        when(fileNodeRepository.selectById("video1")).thenReturn(null);

        String result = mediaService.getPlayVideoJson(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetPlayVideoJson_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("video1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.mp4");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("video1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("video1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = mediaService.getPlayVideoJson(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetPlayVideoJson_Success_MP4_NoFFmpeg() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("video1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.mp4");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("video1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("video1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(kiftdFFMPEGLocator.isEnableFFmpeg()).thenReturn(false);

            String result = mediaService.getPlayVideoJson(request);

            assertNotNull(result);
            VideoInfo vi = gson.fromJson(result, VideoInfo.class);
            assertNotNull(vi);
            assertEquals("N", vi.getNeedEncode());
        }
    }

    @Test
    void testGetPlayVideoJson_Success_AVI() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("video1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.avi");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fileId")).thenReturn("video1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("video1")).thenReturn(node);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(kiftdFFMPEGLocator.isEnableFFmpeg()).thenReturn(true);

            String result = mediaService.getPlayVideoJson(request);

            assertNotNull(result);
            VideoInfo vi = gson.fromJson(result, VideoInfo.class);
            assertNotNull(vi);
            assertEquals("Y", vi.getNeedEncode());
        }
    }
}
