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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.model.Propertie;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.infrastructure.crypto.CryptoService;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.newcore.repository.PropertiesRepository;
import kohgylw.kiftd.server.util.ChainKeyMaster;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;

import java.util.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemServiceImplTest {

    @Mock
    private FileNodeRepository fileNodeRepository;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private PropertiesRepository propertiesRepository;
    @Mock
    private CryptoService cryptoService;
    @Mock
    private ChainKeyMaster chainKeyMaster;
    @Mock
    private FolderUtil folderUtil;
    @Mock
    private LogUtil logUtil;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    private SystemServiceImpl systemService;

    @BeforeEach
    void setUp() throws Exception {
        systemService = new SystemServiceImpl(fileNodeRepository, folderRepository, propertiesRepository, cryptoService, chainKeyMaster, folderUtil, logUtil);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testGetOSName() {
        String osName = systemService.getOSName();
        assertNotNull(osName);
        assertEquals(System.getProperty("os.name"), osName);
    }

    @Test
    void testGetFileChainKey_ChainClosed() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(false);

            String result = systemService.getFileChainKey(request);

            assertNull(result);
        }
    }

    @Test
    void testGetFileChainKey_NullFid() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);
            when(request.getParameter("fid")).thenReturn(null);

            String result = systemService.getFileChainKey(request);

            assertNull(result);
        }
    }

    @Test
    void testGetFileChainKey_FileNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);
            when(request.getParameter("fid")).thenReturn("file1");
            when(fileNodeRepository.selectById("file1")).thenReturn(null);

            String result = systemService.getFileChainKey(request);

            assertNull(result);
        }
    }

    @Test
    void testGetFileChainKey_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(reader.isOpenFileChain()).thenReturn(true);
            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = systemService.getFileChainKey(request);

            assertNull(result);
        }
    }

    @Test
    void testGetFileChainKey_FolderNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(reader.isOpenFileChain()).thenReturn(true);
            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(null);

            String result = systemService.getFileChainKey(request);

            assertNull(result);
        }
    }

    @Test
    void testGetFileChainKey_NoAccess() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(reader.isOpenFileChain()).thenReturn(true);
            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(false);

            String result = systemService.getFileChainKey(request);

            assertNull(result);
        }
    }

    @Test
    void testGetFileChainKey_Success_ExistingKey() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            Propertie keyProp = new Propertie();
            keyProp.setPropertieKey("chain_aes_key");
            keyProp.setPropertieValue("wrappedKey");

            when(reader.isOpenFileChain()).thenReturn(true);
            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(propertiesRepository.selectByKey("chain_aes_key")).thenReturn(keyProp);
            when(chainKeyMaster.unwrap("wrappedKey")).thenReturn("realAesKey");
            when(cryptoService.encrypt("realAesKey", "file1")).thenReturn("encryptedKey123");

            String result = systemService.getFileChainKey(request);

            assertNotNull(result);
            assertTrue(result.length() > 0);
        }
    }

    @Test
    void testGetFileChainKey_Success_NewKey() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(reader.isOpenFileChain()).thenReturn(true);
            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(fileNodeRepository.selectById("file1")).thenReturn(node);
            when(folderUtil.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(folderRepository.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(any(Folder.class), anyString())).thenReturn(true);
            when(propertiesRepository.selectByKey("chain_aes_key")).thenReturn(null);
            when(cryptoService.generateRandomAesKey()).thenReturn("newAesKey");
            when(chainKeyMaster.wrap("newAesKey")).thenReturn("wrappedNewKey");
            when(propertiesRepository.insert(any(Propertie.class))).thenReturn(1);
            when(cryptoService.encrypt("newAesKey", "file1")).thenReturn("encryptedNewKey123");

            String result = systemService.getFileChainKey(request);

            assertNotNull(result);
            assertTrue(result.length() > 0);
            verify(propertiesRepository, times(1)).insert(any(Propertie.class));
        }
    }
}
