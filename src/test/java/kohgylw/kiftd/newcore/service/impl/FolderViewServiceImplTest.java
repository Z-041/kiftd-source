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
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.FolderView;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;

import java.util.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FolderViewServiceImplTest {

    @Mock
    private FolderRepository folderRepository;
    @Mock
    private FileNodeRepository fileNodeRepository;
    @Mock
    private FolderUtil folderUtil;
    private Gson gson;
    @Mock
    private KiftdFFMPEGLocator kiftdFFMPEGLocator;
    @Mock
    private HttpSession session;
    @Mock
    private HttpServletRequest request;

    private FolderViewServiceImpl folderViewService;

    @BeforeEach
    void setUp() throws Exception {
        gson = new Gson();
        folderViewService = new FolderViewServiceImpl(folderRepository, fileNodeRepository, folderUtil, gson, kiftdFFMPEGLocator);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testGetFolderViewJson_NullFid() {
        String result = folderViewService.getFolderViewJson(null, session, request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetFolderViewJson_EmptyFid() {
        String result = folderViewService.getFolderViewJson("", session, request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetFolderViewJson_FolderNotFound() {
        when(folderRepository.selectById("folder1")).thenReturn(null);

        String result = folderViewService.getFolderViewJson("folder1", session, request);

        assertEquals("NOT_FOUND", result);
    }

    @Test
    void testGetFolderViewJson_NotAccess() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(false);

            String result = folderViewService.getFolderViewJson("folder1", session, request);

            assertEquals("notAccess", result);
        }
    }

    @Test
    void testGetFolderViewJson_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            folder.setFolderName("testfolder");
            List<Folder> parentList = new ArrayList<>();
            List<Folder> subFolders = new ArrayList<>();
            List<Node> files = new ArrayList<>();

            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(folderUtil.getParentList("folder1")).thenReturn(parentList);
            when(folderRepository.countByParentId("folder1")).thenReturn(0L);
            lenient().when(folderRepository.selectByParentIdSection(anyMap())).thenReturn(subFolders);
            when(fileNodeRepository.selectByParentFolderIdSection(anyMap())).thenReturn(files);
            lenient().when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = folderViewService.getFolderViewJson("folder1", session, request);

            assertNotNull(result);
            FolderView fv = gson.fromJson(result, FolderView.class);
            assertNotNull(fv);
            assertEquals("testfolder", fv.getFolder().getFolderName());
        }
    }

    @Test
    void testGetSearchViewJson_NullFid() {
        when(request.getParameter("fid")).thenReturn(null);
        when(request.getParameter("keyworld")).thenReturn("test");

        String result = folderViewService.getSearchViewJson(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetSearchViewJson_NullKeyworld() {
        when(request.getParameter("fid")).thenReturn("folder1");
        when(request.getParameter("keyworld")).thenReturn(null);

        String result = folderViewService.getSearchViewJson(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetSearchViewJson_EmptyKeyworld() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            folder.setFolderName("testfolder");

            when(request.getParameter("fid")).thenReturn("folder1");
            when(request.getParameter("keyworld")).thenReturn("");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(folderUtil.getParentList("folder1")).thenReturn(new ArrayList<>());
            when(folderRepository.countByParentId("folder1")).thenReturn(0L);
            lenient().when(folderRepository.selectByParentIdSection(anyMap())).thenReturn(new ArrayList<>());
            when(fileNodeRepository.selectByParentFolderIdSection(anyMap())).thenReturn(new ArrayList<>());
            lenient().when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = folderViewService.getSearchViewJson(request);

            assertNotNull(result);
        }
    }

    @Test
    void testGetSearchViewJson_FolderNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            when(request.getParameter("fid")).thenReturn("folder1");
            when(request.getParameter("keyworld")).thenReturn("test");
            when(folderRepository.selectById("folder1")).thenReturn(null);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");

            String result = folderViewService.getSearchViewJson(request);

            assertEquals("notAccess", result);
        }
    }

    @Test
    void testGetSearchViewJson_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            folder.setFolderName("testfolder");
            List<Folder> allFolders = new ArrayList<>();
            List<Node> allFiles = new ArrayList<>();

            when(request.getParameter("fid")).thenReturn("folder1");
            when(request.getParameter("keyworld")).thenReturn("test");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(folderUtil.getParentList("folder1")).thenReturn(new ArrayList<>());
            when(folderUtil.getAllDescendantFolders("folder1")).thenReturn(allFolders);
            when(fileNodeRepository.selectByParentFolderIds(anyList())).thenReturn(allFiles);
            lenient().when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = folderViewService.getSearchViewJson(request);

            assertNotNull(result);
            assertTrue(result.contains("testfolder"));
        }
    }

    @Test
    void testGetRemainingFolderViewJson_NullFid() {
        when(request.getParameter("fid")).thenReturn(null);

        String result = folderViewService.getRemainingFolderViewJson(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetRemainingFolderViewJson_EmptyFid() {
        when(request.getParameter("fid")).thenReturn("");

        String result = folderViewService.getRemainingFolderViewJson(request);

        assertEquals("ERROR", result);
    }

    @Test
    void testGetRemainingFolderViewJson_FolderNotFound() {
        when(request.getParameter("fid")).thenReturn("folder1");
        when(folderRepository.selectById("folder1")).thenReturn(null);

        String result = folderViewService.getRemainingFolderViewJson(request);

        assertEquals("NOT_FOUND", result);
    }
}
