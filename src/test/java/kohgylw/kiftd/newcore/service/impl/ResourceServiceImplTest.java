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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.NoticeUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.util.TxtCharsetGetter;
import kohgylw.kiftd.server.util.VideoTranscodeUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceServiceImplTest {

    @Mock
    private NodeMapper nm;
    @Mock
    private FileBlockUtil fbu;
    @Mock
    private LogUtil lu;
    @Mock
    private VideoTranscodeUtil vtu;
    @Mock
    private FolderUtil fu;
    @Mock
    private FolderMapper fm;
    @Mock
    private NoticeUtil nu;
    @Mock
    private TxtCharsetGetter tcg;
    @Mock
    private ContentTypeMap ctm;
    @Mock
    private KiftdFFMPEGLocator kfl;
    @Mock
    private IpAddrGetter idg;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private ServletOutputStream servletOutputStream;
    @Mock
    private PrintWriter printWriter;
    @Mock
    private Printer printer;

    private ResourceServiceImpl resourceService;

    @BeforeEach
    void setUp() throws Exception {
        Printer.instance = printer;
        resourceService = new ResourceServiceImpl(nm, fbu, lu, vtu, fu, fm, nu, tcg, ctm, kfl, idg);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testGetResource_NullFid() throws Exception {
        resourceService.getResource(null, request, response);

        verify(response, times(1)).sendError(404);
    }

    @Test
    void testGetResource_NodeNotFound() throws Exception {
        when(nm.selectById("file1")).thenReturn(null);

        resourceService.getResource("file1", request, response);

        verify(response, times(1)).sendError(404);
    }

    @Test
    void testGetResource_NoAuthorized() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);
            lenient().when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);

            resourceService.getResource("file1", request, response);

            verify(response, times(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Test
    void testGetResource_UnauthorizedIOException() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            doThrow(new IOException()).when(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);

            resourceService.getResource("file1", request, response);

            verify(lu, times(1)).writeException(any(IOException.class));
        }
    }

    @Test
    void testGetResource_FileNotExist() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.txt");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);
            when(fbu.getFileFromBlocks(node)).thenReturn(null);

            resourceService.getResource("file1", request, response);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testGetResource_TextFile_Success() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<ServerTimeUtil> stuMock = mockStatic(ServerTimeUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            stuMock.when(() -> ServerTimeUtil.getLastModifiedFormBlock(any(File.class))).thenReturn("last-modified-date");

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.txt");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            File tempFile = File.createTempFile("test", ".txt");
            tempFile.deleteOnExit();

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);
            when(fbu.getFileFromBlocks(node)).thenReturn(tempFile);
            when(ctm.getContentType(".txt")).thenReturn("text/plain");
            when(idg.getIpAddr(request)).thenReturn("127.0.0.1");
            when(response.getOutputStream()).thenReturn(servletOutputStream);
            when(reader.getBuffSize()).thenReturn(8192);
            when(reader.getDownloadMaxRate("user1")).thenReturn(-1L);
            when(fbu.getETag(tempFile)).thenReturn("etag123");

            resourceService.getResource("file1", request, response);

            verify(lu, times(1)).writeDownloadFileEvent(eq("user1"), eq("127.0.0.1"), eq(node));
        }
    }

    @Test
    void testGetResource_VideoMP4_FFmpegDisabled() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<ServerTimeUtil> stuMock = mockStatic(ServerTimeUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            stuMock.when(() -> ServerTimeUtil.getLastModifiedFormBlock(any(File.class))).thenReturn("last-modified-date");

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("video.mp4");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            File tempFile = File.createTempFile("video", ".mp4");
            tempFile.deleteOnExit();

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);
            when(fbu.getFileFromBlocks(node)).thenReturn(tempFile);
            when(kfl.isEnableFFmpeg()).thenReturn(false);
            when(ctm.getContentType(".mp4")).thenReturn("video/mp4");
            when(idg.getIpAddr(request)).thenReturn("127.0.0.1");
            when(response.getOutputStream()).thenReturn(servletOutputStream);
            when(reader.getBuffSize()).thenReturn(8192);
            when(reader.getDownloadMaxRate("user1")).thenReturn(-1L);
            when(fbu.getETag(tempFile)).thenReturn("etag123");

            resourceService.getResource("file1", request, response);

            verify(lu, times(1)).writeDownloadFileEvent(eq("user1"), eq("127.0.0.1"), eq(node));
        }
    }

    @Test
    void testGetVideoTranscodeStatus_FFmpegDisabled() {
        when(kfl.isEnableFFmpeg()).thenReturn(false);

        String result = resourceService.getVideoTranscodeStatus(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetVideoTranscodeStatus_NullFileId() {
        when(kfl.isEnableFFmpeg()).thenReturn(true);
        when(request.getParameter("fileId")).thenReturn(null);

        String result = resourceService.getVideoTranscodeStatus(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetVideoTranscodeStatus_Success() throws Exception {
        when(kfl.isEnableFFmpeg()).thenReturn(true);
        when(request.getParameter("fileId")).thenReturn("file1");
        when(vtu.getTranscodeProcess("file1")).thenReturn("50%");

        String result = resourceService.getVideoTranscodeStatus(request);

        assertEquals("50%", result);
    }

    @Test
    void testGetVideoTranscodeStatus_Exception() throws Exception {
        when(kfl.isEnableFFmpeg()).thenReturn(true);
        when(request.getParameter("fileId")).thenReturn("file1");
        when(vtu.getTranscodeProcess("file1")).thenThrow(new RuntimeException("error"));

        String result = resourceService.getVideoTranscodeStatus(request);

        assertEquals("ERROR", result);
        verify(lu, times(1)).writeException(any(Exception.class));
    }

    @Test
    void testGetLRContextByUTF8_NullFileId() throws Exception {
        resourceService.getLRContextByUTF8(null, request, response);

        verify(response, times(1)).sendError(500);
    }

    @Test
    void testGetLRContextByUTF8_NodeNotFound() throws Exception {
        when(nm.selectById("file1")).thenReturn(null);

        resourceService.getLRContextByUTF8("file1", request, response);

        verify(response, times(1)).sendError(500);
    }

    @Test
    void testGetLRContextByUTF8_NoAuthorized() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);
            lenient().when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);

            resourceService.getLRContextByUTF8("file1", request, response);

            verify(response, times(1)).sendError(500);
        }
    }

    @Test
    void testGetLRContextByUTF8_NotLRCFile() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.txt");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            File tempFile = File.createTempFile("test", ".txt");
            tempFile.deleteOnExit();

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);
            when(fbu.getFileFromBlocks(node)).thenReturn(tempFile);
            when(idg.getIpAddr(request)).thenReturn("127.0.0.1");

            resourceService.getLRContextByUTF8("file1", request, response);

            verify(response, times(1)).sendError(500);
        }
    }

    @Test
    void testGetLRContextByUTF8_304_IfModifiedSince() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<ServerTimeUtil> stuMock = mockStatic(ServerTimeUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            stuMock.when(() -> ServerTimeUtil.getLastModifiedFormBlock(any(File.class))).thenReturn("cached-date");

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.lrc");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            File tempFile = File.createTempFile("test", ".lrc");
            tempFile.deleteOnExit();

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);
            when(fbu.getFileFromBlocks(node)).thenReturn(tempFile);
            when(idg.getIpAddr(request)).thenReturn("127.0.0.1");
            when(request.getHeader("If-Modified-Since")).thenReturn("cached-date");

            resourceService.getLRContextByUTF8("file1", request, response);

            verify(response, times(1)).setStatus(304);
        }
    }

    @Test
    void testGetLRContextByUTF8_304_IfNoneMatch() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<ServerTimeUtil> stuMock = mockStatic(ServerTimeUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            stuMock.when(() -> ServerTimeUtil.getLastModifiedFormBlock(any(File.class))).thenReturn("last-modified");

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.lrc");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            File tempFile = File.createTempFile("test", ".lrc");
            tempFile.deleteOnExit();

            when(nm.selectById("file1")).thenReturn(node);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);
            when(fbu.getFileFromBlocks(node)).thenReturn(tempFile);
            when(fbu.getETag(tempFile)).thenReturn("etag123");
            when(idg.getIpAddr(request)).thenReturn("127.0.0.1");
            when(request.getHeader("If-None-Match")).thenReturn("etag123");

            resourceService.getLRContextByUTF8("file1", request, response);

            verify(response, times(1)).setStatus(304);
        }
    }

    @Test
    void testGetLRContextByUTF8_SendErrorIOException() throws Exception {
        when(nm.selectById("file1")).thenReturn(null);
        doThrow(new IOException()).when(response).sendError(500);

        resourceService.getLRContextByUTF8("file1", request, response);

        verify(lu, times(1)).writeException(any(IOException.class));
    }

    @Test
    void testGetNoticeContext_NoticeFileNotExist() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getTemporaryfilePath()).thenReturn(System.getProperty("java.io.tmpdir"));
            when(response.getWriter()).thenReturn(printWriter);

            resourceService.getNoticeContext(request, response);

            verify(response, times(1)).setContentType("text/html");
            verify(response, times(1)).setCharacterEncoding("UTF-8");
        }
    }

    @Test
    void testGetNoticeMD5() {
        when(nu.getMd5()).thenReturn("md5hash123");

        String result = resourceService.getNoticeMD5();

        assertEquals("md5hash123", result);
        verify(nu, times(1)).getMd5();
    }
}
