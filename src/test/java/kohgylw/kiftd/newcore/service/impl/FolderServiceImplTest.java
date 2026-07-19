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
import kohgylw.kiftd.server.pojo.CreateNewFolderByNameRespons;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.util.TextFormateUtil;

import java.util.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FolderServiceImplTest {

    @Mock
    private FolderRepository folderRepository;
    @Mock
    private FileNodeRepository fileNodeRepository;
    @Mock
    private FolderUtil folderUtil;
    @Mock
    private LogUtil logUtil;
    private Gson gson;
    @Mock
    private IpAddrGetter ipAddrGetter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    private FolderServiceImpl folderService;

    @BeforeEach
    void setUp() throws Exception {
        gson = new Gson();
        folderService = new FolderServiceImpl(folderRepository, fileNodeRepository, folderUtil, logUtil, gson, ipAddrGetter);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testNewFolder_NullParentId() {
        String result = folderService.newFolder(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testNewFolder_EmptyFolderName() {
        when(request.getParameter("parentId")).thenReturn("folder1");
        when(request.getParameter("folderName")).thenReturn("");
        String result = folderService.newFolder(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testNewFolder_InvalidFolderName() {
        try (MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("invalid/name");
            when(textFormateUtil.matcherFolderName("invalid/name")).thenReturn(false);

            String result = folderService.newFolder(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testNewFolder_NullFolderConstraint() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class);
             MockedStatic<ServerTimeUtil> stuMock = mockStatic(ServerTimeUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);
            stuMock.when(() -> ServerTimeUtil.accurateToDay()).thenReturn("2024-01-01");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("folder1");
            parentFolder.setFolderConstraint(0);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("newfolder");
            lenient().when(request.getParameter("folderConstraint")).thenReturn(null);
            when(textFormateUtil.matcherFolderName("newfolder")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(parentFolder);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);

            String result = folderService.newFolder(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testNewFolder_ParentNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("newfolder");
            when(request.getParameter("folderConstraint")).thenReturn("0");
            when(textFormateUtil.matcherFolderName("newfolder")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(null);

            String result = folderService.newFolder(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testNewFolder_NoAccess() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("folder1");
            parentFolder.setFolderConstraint(0);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("newfolder");
            when(request.getParameter("folderConstraint")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(textFormateUtil.matcherFolderName("newfolder")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(parentFolder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(false);

            String result = folderService.newFolder(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testNewFolder_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("folder1");
            parentFolder.setFolderConstraint(0);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("newfolder");
            when(request.getParameter("folderConstraint")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(textFormateUtil.matcherFolderName("newfolder")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(parentFolder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = folderService.newFolder(request);

            assertEquals("noAuthorized", result);
        }
    }

    @Test
    void testNewFolder_FoldersTotalOutOfLimit() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("folder1");
            parentFolder.setFolderConstraint(0);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("newfolder");
            when(request.getParameter("folderConstraint")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(textFormateUtil.matcherFolderName("newfolder")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(parentFolder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.countByParentId("folder1")).thenReturn(10000L);

            String result = folderService.newFolder(request);

            assertEquals("foldersTotalOutOfLimit", result);
        }
    }

    @Test
    void testNewFolder_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class);
             MockedStatic<ServerTimeUtil> stuMock = mockStatic(ServerTimeUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);
            stuMock.when(() -> ServerTimeUtil.accurateToDay()).thenReturn("2024-01-01");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("folder1");
            parentFolder.setFolderConstraint(0);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("newfolder");
            when(request.getParameter("folderConstraint")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(ipAddrGetter.getIpAddr(request)).thenReturn("127.0.0.1");
            when(textFormateUtil.matcherFolderName("newfolder")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(parentFolder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.countByParentId("folder1")).thenReturn(0L);
            when(folderRepository.selectByParentId("folder1")).thenReturn(new ArrayList<>());
            when(folderRepository.insert(any(Folder.class))).thenReturn(1);
            when(folderUtil.isValidFolder(any(Folder.class))).thenReturn(true);

            String result = folderService.newFolder(request);

            assertEquals("createFolderSuccess", result);
            verify(folderRepository, times(1)).insert(any(Folder.class));
            verify(logUtil, times(1)).writeCreateFolderEvent(eq("user1"), eq("127.0.0.1"), any(Folder.class));
        }
    }

    @Test
    void testDeleteFolder_NullFolderId() {
        String result = folderService.deleteFolder(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testDeleteFolder_EmptyFolderId() {
        when(request.getParameter("folderId")).thenReturn("");
        String result = folderService.deleteFolder(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testDeleteFolder_RootFolderId() {
        when(request.getParameter("folderId")).thenReturn("root");
        String result = folderService.deleteFolder(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testDeleteFolder_FolderNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(folderRepository.selectById("folder1")).thenReturn(null);

            String result = folderService.deleteFolder(request);

            assertEquals("deleteFolderSuccess", result);
        }
    }

    @Test
    void testDeleteFolder_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            folder.setFolderParent("parent1");

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("parent1")).thenReturn(Collections.singletonList("parent1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = folderService.deleteFolder(request);

            assertEquals("noAuthorized", result);
        }
    }

    @Test
    void testDeleteFolder_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            folder.setFolderParent("parent1");
            folder.setFolderName("testfolder");
            List<Folder> parentList = new ArrayList<>();

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("parent1")).thenReturn(Collections.singletonList("parent1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderUtil.getParentList("folder1")).thenReturn(parentList);
            when(folderRepository.deleteById("folder1")).thenReturn(1);

            String result = folderService.deleteFolder(request);

            assertEquals("deleteFolderSuccess", result);
            verify(folderUtil, times(1)).deleteAllChildFolder("folder1");
            verify(logUtil, times(1)).writeDeleteFolderEvent(eq(request), eq(folder), eq(parentList));
        }
    }

    @Test
    void testDeleteFolder_Failure() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            folder.setFolderParent("parent1");

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(folderUtil.getAllFoldersId("parent1")).thenReturn(Collections.singletonList("parent1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.deleteById("folder1")).thenReturn(0);

            String result = folderService.deleteFolder(request);

            assertEquals("cannotDeleteFolder", result);
        }
    }

    @Test
    void testDeleteFolderByName_NullParentId() {
        when(request.getParameter("parentId")).thenReturn(null);
        String result = folderService.deleteFolderByName(request);
        assertEquals("deleteError", result);
    }

    @Test
    void testDeleteFolderByName_ParentNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(folderRepository.selectById("folder1")).thenReturn(null);

            String result = folderService.deleteFolderByName(request);

            assertEquals("deleteError", result);
        }
    }

    @Test
    void testDeleteFolderByName_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("folder1");
            Folder targetFolder = new Folder();
            targetFolder.setFolderId("folder2");
            targetFolder.setFolderName("testfolder");
            targetFolder.setFolderParent("folder1");
            List<Folder> subFolders = Collections.singletonList(targetFolder);
            List<Folder> parentList = new ArrayList<>();

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("testfolder");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(parentFolder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.selectByParentId("folder1")).thenReturn(subFolders);
            when(folderUtil.getParentList("folder2")).thenReturn(parentList);
            when(folderRepository.deleteById("folder2")).thenReturn(1);

            String result = folderService.deleteFolderByName(request);

            assertEquals("deleteSuccess", result);
        }
    }

    @Test
    void testRenameFolder_NullFolderId() {
        String result = folderService.renameFolder(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testRenameFolder_NullNewName() {
        when(request.getParameter("folderId")).thenReturn("folder1");
        String result = folderService.renameFolder(request);
        assertEquals("errorParameter", result);
    }

    @Test
    void testRenameFolder_InvalidName() {
        try (MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("newName")).thenReturn("invalid/name");
            when(textFormateUtil.matcherFolderName("invalid/name")).thenReturn(false);

            String result = folderService.renameFolder(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testRenameFolder_FolderNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("newName")).thenReturn("newname");
            when(textFormateUtil.matcherFolderName("newname")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(null);

            String result = folderService.renameFolder(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testRenameFolder_NullFolderConstraint() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<TextFormateUtil> tfuMock = mockStatic(TextFormateUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            TextFormateUtil textFormateUtil = mock(TextFormateUtil.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            tfuMock.when(TextFormateUtil::instance).thenReturn(textFormateUtil);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            folder.setFolderParent("parent1");
            folder.setFolderName("oldname");

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(request.getParameter("newName")).thenReturn("newname");
            lenient().when(request.getParameter("folderConstraint")).thenReturn(null);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(textFormateUtil.matcherFolderName("newname")).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(folderUtil.getAllFoldersId("parent1")).thenReturn(Collections.singletonList("parent1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.selectById("parent1")).thenReturn(new Folder());

            String result = folderService.renameFolder(request);

            assertEquals("errorParameter", result);
        }
    }

    @Test
    void testCreateNewFolderByName_NullParentId() {
        String result = folderService.createNewFolderByName(request);
        CreateNewFolderByNameRespons resp = gson.fromJson(result, CreateNewFolderByNameRespons.class);
        assertEquals("error", resp.getResult());
    }

    @Test
    void testCreateNewFolderByName_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<ServerTimeUtil> stuMock = mockStatic(ServerTimeUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            stuMock.when(() -> ServerTimeUtil.accurateToDay()).thenReturn("2024-01-01");

            Folder parentFolder = new Folder();
            parentFolder.setFolderId("folder1");
            parentFolder.setFolderConstraint(0);

            when(request.getParameter("parentId")).thenReturn("folder1");
            when(request.getParameter("folderName")).thenReturn("newfolder");
            when(request.getParameter("folderConstraint")).thenReturn("0");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(ipAddrGetter.getIpAddr(request)).thenReturn("127.0.0.1");
            when(folderRepository.selectById("folder1")).thenReturn(parentFolder);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.countByParentId("folder1")).thenReturn(0L);
            when(folderRepository.selectByParentId("folder1")).thenReturn(new ArrayList<>());
            when(folderRepository.insert(any(Folder.class))).thenReturn(1);
            when(folderUtil.isValidFolder(any(Folder.class))).thenReturn(true);

            String result = folderService.createNewFolderByName(request);

            assertNotNull(result);
            CreateNewFolderByNameRespons resp = gson.fromJson(result, CreateNewFolderByNameRespons.class);
            assertEquals("success", resp.getResult());
            assertEquals("newfolder", resp.getNewName());
        }
    }

    @Test
    void testGetFolderCountResult_NullFolderId() {
        when(request.getParameter("folderId")).thenReturn(null);
        String result = folderService.getFolderCountResult(request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetFolderCountResult_EmptyFolderId() {
        when(request.getParameter("folderId")).thenReturn("");
        String result = folderService.getFolderCountResult(request);
        assertEquals("ERROR", result);
    }

    @Test
    void testGetFolderCountResult_FolderNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(folderRepository.selectById("folder1")).thenReturn(null);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");

            String result = folderService.getFolderCountResult(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetFolderCountResult_NoAccess() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(false);

            String result = folderService.getFolderCountResult(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetFolderCountResult_Success() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Folder folder = new Folder();
            folder.setFolderId("folder1");
            Folder subFolder1 = new Folder();
            subFolder1.setFolderId("folder2");
            subFolder1.setFolderParent("folder1");
            Folder subFolder2 = new Folder();
            subFolder2.setFolderId("folder3");
            subFolder2.setFolderParent("folder1");
            List<Folder> subFolders = Arrays.asList(subFolder1, subFolder2);
            
            kohgylw.kiftd.server.model.Node file1 = new kohgylw.kiftd.server.model.Node();
            file1.setFileSize("1024");
            kohgylw.kiftd.server.model.Node file2 = new kohgylw.kiftd.server.model.Node();
            file2.setFileSize("2048");
            kohgylw.kiftd.server.model.Node file3 = new kohgylw.kiftd.server.model.Node();
            file3.setFileSize("4096");
            List<kohgylw.kiftd.server.model.Node> files = Arrays.asList(file1, file2, file3);

            when(request.getParameter("folderId")).thenReturn("folder1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(folderUtil.getAllDescendantFolders("folder1")).thenReturn(subFolders);
            when(fileNodeRepository.selectByParentFolderIds(anyList())).thenReturn(files);

            String result = folderService.getFolderCountResult(request);

            assertNotNull(result);
            assertTrue(result.contains("2"));
            assertTrue(result.contains("3"));
        }
    }
}
