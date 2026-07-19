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
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalDownloadServiceImplTest {

    @Mock
    private FileNodeRepository nm;
    @Mock
    private LogUtil lu;
    @Mock
    private FileBlockUtil fbu;
    @Mock
    private FolderUtil fu;
    @Mock
    private FolderRepository fm;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private ServletOutputStream servletOutputStream;

    private ExternalDownloadServiceImpl externalDownloadService;

    @BeforeEach
    void setUp() throws Exception {
        externalDownloadService = new ExternalDownloadServiceImpl(nm, fm, lu, fbu, fu);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testGetDownloadKey_NullFileId() {
        when(request.getParameter("fId")).thenReturn(null);

        String result = externalDownloadService.getDownloadKey(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetDownloadKey_NodeNotFound() {
        when(request.getParameter("fId")).thenReturn("file1");
        when(nm.selectById("file1")).thenReturn(null);

        String result = externalDownloadService.getDownloadKey(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetDownloadKey_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);
            lenient().when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);

            String result = externalDownloadService.getDownloadKey(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetDownloadKey_Success_NewKey() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);

            String result = externalDownloadService.getDownloadKey(request);

            assertNotNull(result);
            assertNotEquals("ERROR", result);
            verify(lu, times(1)).writeShareFileURLEvent(eq(request), eq(node));
        }
    }

    @Test
    void testGetDownloadKey_Success_ExistingKey() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(folder);

            String firstKey = externalDownloadService.getDownloadKey(request);
            String secondKey = externalDownloadService.getDownloadKey(request);

            assertNotNull(firstKey);
            assertNotNull(secondKey);
            assertEquals(firstKey, secondKey);
        }
    }

    @Test
    void testDownloadFileByKey_NullDkey() throws Exception {
        when(request.getParameter("dkey")).thenReturn(null);

        externalDownloadService.downloadFileByKey(request, response);

        verify(response, times(1)).sendError(404);
    }

    @Test
    void testDownloadFileByKey_InvalidKey() throws Exception {
        when(request.getParameter("dkey")).thenReturn("invalid-key");

        externalDownloadService.downloadFileByKey(request, response);

        verify(response, times(1)).sendError(404);
    }

    @Test
    void testDownloadFileByKey_NodeNotFound() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getDownloadMaxRate(null)).thenReturn(-1L);
            when(fbu.getETag(any(File.class))).thenReturn("etag123");

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.txt");

            when(request.getParameter("fId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(new Folder());

            String dkey = externalDownloadService.getDownloadKey(request);

            when(request.getParameter("dkey")).thenReturn(dkey);
            when(nm.selectById("file1")).thenReturn(null);

            externalDownloadService.downloadFileByKey(request, response);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testDownloadFileByKey_FileNotExist() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getDownloadMaxRate(null)).thenReturn(-1L);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            node.setFileName("test.txt");

            when(request.getParameter("fId")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(fm.selectById("folder1")).thenReturn(new Folder());

            String dkey = externalDownloadService.getDownloadKey(request);

            when(request.getParameter("dkey")).thenReturn(dkey);
            when(nm.selectById("file1")).thenReturn(node);
            when(fbu.getFileFromBlocks(node)).thenReturn(null);

            externalDownloadService.downloadFileByKey(request, response);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testDownloadFileByKey_SendErrorIOException() throws Exception {
        when(request.getParameter("dkey")).thenReturn(null);
        doThrow(new IOException()).when(response).sendError(404);

        externalDownloadService.downloadFileByKey(request, response);

        verify(lu, times(1)).writeException(any(IOException.class));
    }

    @Test
    void testCleanExpiredKeys() throws Exception {
        Method method = ExternalDownloadServiceImpl.class.getDeclaredMethod("cleanExpiredKeys");
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(null));
    }
}
