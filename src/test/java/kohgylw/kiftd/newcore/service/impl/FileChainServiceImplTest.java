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
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.model.Propertie;
import kohgylw.kiftd.server.util.AESCipher;
import kohgylw.kiftd.server.util.ChainKeyMaster;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;

import java.io.IOException;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileChainServiceImplTest {

    @Mock
    private NodeMapper nm;
    @Mock
    private FolderMapper flm;
    @Mock
    private FileBlockUtil fbu;
    @Mock
    private ContentTypeMap ctm;
    @Mock
    private LogUtil lu;
    @Mock
    private AESCipher cipher;
    @Mock
    private ChainKeyMaster chainKeyMaster;
    @Mock
    private PropertiesMapper pm;
    @Mock
    private FolderUtil fu;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private ServletOutputStream servletOutputStream;

    private FileChainServiceImpl fileChainService;

    @BeforeEach
    void setUp() throws Exception {
        fileChainService = new FileChainServiceImpl(nm, flm, fbu, ctm, lu, cipher, chainKeyMaster, pm, fu);
        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testGetResourceByChainKey_FileChainDisabled() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(false);

            fileChainService.getResourceByChainKey(request, response);

            verify(response, times(1)).sendError(403);
        }
    }

    @Test
    void testGetResourceByChainKey_NullCkey() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            when(request.getParameter("ckey")).thenReturn(null);

            fileChainService.getResourceByChainKey(request, response);

            verify(response, times(1)).sendError(403);
        }
    }

    @Test
    void testGetResourceByChainKey_NoKeyProp() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            when(request.getParameter("ckey")).thenReturn("someckey");
            when(pm.selectByKey("chain_aes_key")).thenReturn(null);

            fileChainService.getResourceByChainKey(request, response);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testGetResourceByChainKey_NodeNotFound() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Propertie keyProp = new Propertie();
            keyProp.setPropertieKey("chain_aes_key");
            keyProp.setPropertieValue("wrappedkey");

            when(request.getParameter("ckey")).thenReturn("someckey");
            when(pm.selectByKey("chain_aes_key")).thenReturn(keyProp);
            when(chainKeyMaster.unwrap("wrappedkey")).thenReturn("aeskey");
            when(cipher.decrypt("aeskey", "someckey")).thenReturn("file1");
            when(nm.selectById("file1")).thenReturn(null);

            fileChainService.getResourceByChainKey(request, response);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testGetResourceByChainKey_FileNotExist() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Propertie keyProp = new Propertie();
            keyProp.setPropertieKey("chain_aes_key");
            keyProp.setPropertieValue("wrappedkey");
            Node node = new Node();
            node.setFileId("file1");

            when(request.getParameter("ckey")).thenReturn("someckey");
            when(pm.selectByKey("chain_aes_key")).thenReturn(keyProp);
            when(chainKeyMaster.unwrap("wrappedkey")).thenReturn("aeskey");
            when(cipher.decrypt("aeskey", "someckey")).thenReturn("file1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fbu.getFileFromBlocks(node)).thenReturn(null);

            fileChainService.getResourceByChainKey(request, response);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testGetResourceByChainKey_DecryptException() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Propertie keyProp = new Propertie();
            keyProp.setPropertieKey("chain_aes_key");
            keyProp.setPropertieValue("wrappedkey");

            when(request.getParameter("ckey")).thenReturn("invalidckey");
            when(pm.selectByKey("chain_aes_key")).thenReturn(keyProp);
            when(chainKeyMaster.unwrap("wrappedkey")).thenReturn("aeskey");
            when(cipher.decrypt("aeskey", "invalidckey")).thenThrow(new RuntimeException("decrypt error"));

            fileChainService.getResourceByChainKey(request, response);

            verify(response, times(1)).sendError(500);
            verify(lu, times(1)).writeException(any(Exception.class));
        }
    }

    @Test
    void testGetResourceByChainKey_SendErrorIOException() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(false);

            doThrow(new IOException()).when(response).sendError(403);

            fileChainService.getResourceByChainKey(request, response);

            verify(lu, times(1)).writeException(any(IOException.class));
        }
    }

    @Test
    void testGetChainKeyByFid_FileChainDisabled() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(false);

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetChainKeyByFid_NullFid() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            when(request.getParameter("fid")).thenReturn(null);

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetChainKeyByFid_NodeNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            when(request.getParameter("fid")).thenReturn("file1");
            when(nm.selectById("file1")).thenReturn(null);

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetChainKeyByFid_NoAuthorized() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(false);

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetChainKeyByFid_FolderNotFound() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(flm.selectById("folder1")).thenReturn(null);

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetChainKeyByFid_NoAccessFolder() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(flm.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(folder, "user1")).thenReturn(false);

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetChainKeyByFid_NewKey_Success() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(flm.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(folder, "user1")).thenReturn(true);
            when(pm.selectByKey("chain_aes_key")).thenReturn(null);
            when(cipher.generateRandomKey()).thenReturn("newaeskey");
            when(chainKeyMaster.wrap("newaeskey")).thenReturn("wrappednewkey");
            when(pm.insert(any(Propertie.class))).thenReturn(1);
            when(cipher.encrypt("newaeskey", "file1")).thenReturn("encryptedckey");

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("encryptedckey", result);
            verify(pm, times(1)).insert(any(Propertie.class));
        }
    }

    @Test
    void testGetChainKeyByFid_NewKey_InsertFailed() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(flm.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(folder, "user1")).thenReturn(true);
            when(pm.selectByKey("chain_aes_key")).thenReturn(null);
            when(cipher.generateRandomKey()).thenReturn("newaeskey");
            when(chainKeyMaster.wrap("newaeskey")).thenReturn("wrappednewkey");
            when(pm.insert(any(Propertie.class))).thenReturn(0);

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
        }
    }

    @Test
    void testGetChainKeyByFid_ExistingKey_Wrapped() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            Propertie keyProp = new Propertie();
            keyProp.setPropertieKey("chain_aes_key");
            keyProp.setPropertieValue("wrappedkey");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(flm.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(folder, "user1")).thenReturn(true);
            when(pm.selectByKey("chain_aes_key")).thenReturn(keyProp);
            when(chainKeyMaster.isWrapped("wrappedkey")).thenReturn(true);
            when(chainKeyMaster.unwrap("wrappedkey")).thenReturn("aeskey");
            when(cipher.encrypt("aeskey", "file1")).thenReturn("encryptedckey");

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("encryptedckey", result);
        }
    }

    @Test
    void testGetChainKeyByFid_ExistingKey_PlainTextMigration() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");
            Propertie keyProp = new Propertie();
            keyProp.setPropertieKey("chain_aes_key");
            keyProp.setPropertieValue("plainaeskey");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(flm.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(folder, "user1")).thenReturn(true);
            when(pm.selectByKey("chain_aes_key")).thenReturn(keyProp);
            when(chainKeyMaster.isWrapped("plainaeskey")).thenReturn(false);
            when(chainKeyMaster.unwrap("plainaeskey")).thenReturn("plainaeskey");
            when(chainKeyMaster.wrap("plainaeskey")).thenReturn("wrappedkey");
            when(cipher.encrypt("plainaeskey", "file1")).thenReturn("encryptedckey");

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("encryptedckey", result);
            verify(pm, times(1)).update(keyProp);
            assertEquals("wrappedkey", keyProp.getPropertieValue());
        }
    }

    @Test
    void testGetChainKeyByFid_Exception() {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.isOpenFileChain()).thenReturn(true);

            Node node = new Node();
            node.setFileId("file1");
            node.setFileParentFolder("folder1");
            Folder folder = new Folder();
            folder.setFolderId("folder1");

            when(request.getParameter("fid")).thenReturn("file1");
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(nm.selectById("file1")).thenReturn(node);
            when(fu.getAllFoldersId("folder1")).thenReturn(Collections.singletonList("folder1"));
            when(reader.authorized(anyString(), any(AccountAuth.class), anyList())).thenReturn(true);
            when(flm.selectById("folder1")).thenReturn(folder);
            when(reader.accessFolder(folder, "user1")).thenReturn(true);
            when(pm.selectByKey("chain_aes_key")).thenThrow(new RuntimeException("db error"));

            String result = fileChainService.getChainKeyByFid(request);

            assertEquals("ERROR", result);
            verify(lu, times(1)).writeException(any(Exception.class));
        }
    }
}
