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
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.model.Property;
import kohgylw.kiftd.server.util.AESCipher;
import kohgylw.kiftd.server.util.ChainKeyMaster;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.LogUtil;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileChainServiceImplTest {

    @Mock
    private NodeMapper nm;
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
        fileChainService = new FileChainServiceImpl(nm, fbu, ctm, lu, cipher, chainKeyMaster, pm);
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

            Property keyProp = new Property();
            keyProp.setPropertyKey("chain_aes_key");
            keyProp.setPropertyValue("wrappedkey");

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

            Property keyProp = new Property();
            keyProp.setPropertyKey("chain_aes_key");
            keyProp.setPropertyValue("wrappedkey");
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

            Property keyProp = new Property();
            keyProp.setPropertyKey("chain_aes_key");
            keyProp.setPropertyValue("wrappedkey");

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
}
