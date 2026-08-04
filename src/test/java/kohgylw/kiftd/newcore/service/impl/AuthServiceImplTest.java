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
import com.google.gson.GsonBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.VCLevel;
import kohgylw.kiftd.server.pojo.ChangePasswordInfoPojo;
import kohgylw.kiftd.server.pojo.LoginInfoPojo;
import kohgylw.kiftd.server.pojo.SignUpInfoPojo;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.newcore.domain.OperationResult;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RSADecryptUtil;
import kohgylw.kiftd.server.util.RSAKeyUtil;

import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private RSAKeyUtil rsaKeyUtil;
    @Mock
    private LogUtil logUtil;
    @Mock
    private IpAddrGetter ipAddrGetter;
    private Gson gson;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private Printer printer;

    @BeforeEach
    void setUp() {
        gson = new Gson();
        Printer.instance = printer;
    }

    private AuthServiceImpl createAuthService() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);
            return new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);
        }
    }

    @Test
    void testLogout_Success() throws Exception {
        AuthServiceImpl authService = createAuthService();
        authService.logout(session);
        verify(session, times(1)).invalidate();
    }

    @Test
    void testGetPublicKeyJson_Success() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            String publicKey = "testPublicKey";
            when(rsaKeyUtil.getPublicKey()).thenReturn(publicKey);

            String result = authService.getPublicKeyJson();

            assertNotNull(result);
            assertTrue(result.contains("testPublicKey"));
            verify(rsaKeyUtil, times(1)).getPublicKey();
        }
    }

    @Test
    void testDoPong_LoggedIn() throws Exception {
        AuthServiceImpl authService = createAuthService();

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("ACCOUNT")).thenReturn("user1");

        String result = authService.doPong(request);

        assertEquals("pong", result);
    }

    @Test
    void testDoPong_NotLoggedIn() throws Exception {
        AuthServiceImpl authService = createAuthService();

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("ACCOUNT")).thenReturn(null);

        String result = authService.doPong(request);

        assertEquals("", result);
    }

    @Test
    void testIsAllowSignUp_True() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);
            when(reader.isAllowSignUp()).thenReturn(true);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            boolean result = authService.isAllowSignUp();

            assertTrue(result);
        }
    }

    @Test
    void testIsAllowSignUp_False() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);
            when(reader.isAllowSignUp()).thenReturn(false);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            boolean result = authService.isAllowSignUp();

            assertFalse(result);
        }
    }

    @Test
    void testLogin_Success() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.foundAccount("user1")).thenReturn(true);
            when(reader.checkAccountPwd(eq("user1"), eq("password123"))).thenReturn(true);

            LoginInfoPojo loginInfo = new LoginInfoPojo();
            loginInfo.setAccountId("user1");
            loginInfo.setAccountPwd("password123");
            loginInfo.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(loginInfo));
            when(request.getSession(anyBoolean())).thenReturn(session);

            OperationResult result = authService.login(request, session);

            assertTrue(result.isSuccess());
            assertEquals("permitlogin", result.getCode());
            verify(session, times(1)).invalidate();
        }
    }

    @Test
    void testLogin_AccountNotFound() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.foundAccount("nonexistent")).thenReturn(false);
            when(reader.checkAccountPwd(eq("nonexistent"), anyString())).thenReturn(false);

            LoginInfoPojo loginInfo = new LoginInfoPojo();
            loginInfo.setAccountId("nonexistent");
            loginInfo.setAccountPwd("wrongpwd");
            loginInfo.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(loginInfo));

            OperationResult result = authService.login(request, session);

            assertFalse(result.isSuccess());
            assertEquals("accountnotfound", result.getCode());
        }
    }

    @Test
    void testLogin_PasswordError() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.foundAccount("user1")).thenReturn(true);
            when(reader.checkAccountPwd(eq("user1"), eq("wrongpwd"))).thenReturn(false);

            LoginInfoPojo loginInfo = new LoginInfoPojo();
            loginInfo.setAccountId("user1");
            loginInfo.setAccountPwd("wrongpwd");
            loginInfo.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(loginInfo));

            OperationResult result = authService.login(request, session);

            assertFalse(result.isSuccess());
            assertEquals("accountpwderror", result.getCode());
        }
    }

    @Test
    void testLogin_Timeout() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            LoginInfoPojo loginInfo = new LoginInfoPojo();
            loginInfo.setAccountId("user1");
            loginInfo.setAccountPwd("password123");
            loginInfo.setTime(String.valueOf(System.currentTimeMillis() - 60000L));

            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(loginInfo));

            OperationResult result = authService.login(request, session);

            assertFalse(result.isSuccess());
            assertEquals("error", result.getCode());
        }
    }

    @Test
    void testLogin_Exception() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenThrow(new RuntimeException("decrypt error"));

            OperationResult result = authService.login(request, session);

            assertFalse(result.isSuccess());
            assertEquals("error", result.getCode());
            verify(logUtil, times(1)).writeException(any(Exception.class));
        }
    }

    @Test
    void testLogin_WithVCode_Correct() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Standard);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.foundAccount("user1")).thenReturn(true);
            when(reader.checkAccountPwd(eq("user1"), eq("password123"))).thenReturn(true);

            LoginInfoPojo loginInfo = new LoginInfoPojo();
            loginInfo.setAccountId("user1");
            loginInfo.setAccountPwd("password123");
            loginInfo.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(request.getParameter("vercode")).thenReturn("abc1");
            when(session.getAttribute("VERCODE")).thenReturn("abc1");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(loginInfo));
            when(request.getSession(anyBoolean())).thenReturn(session);

            Field focusAccountField = AuthServiceImpl.class.getDeclaredField("focusAccount");
            focusAccountField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<String> focusAccount = (java.util.Set<String>) focusAccountField.get(null);
            focusAccount.add("user1");

            OperationResult result = authService.login(request, session);

            assertTrue(result.isSuccess());
            assertEquals("permitlogin", result.getCode());
            assertFalse(focusAccount.contains("user1"));
        }
    }

    @Test
    void testLogin_WithVCode_Wrong() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Standard);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.foundAccount("user1")).thenReturn(true);
            when(reader.checkAccountPwd(eq("user1"), eq("password123"))).thenReturn(true);

            LoginInfoPojo loginInfo = new LoginInfoPojo();
            loginInfo.setAccountId("user1");
            loginInfo.setAccountPwd("password123");
            loginInfo.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(request.getParameter("vercode")).thenReturn("wrong");
            when(session.getAttribute("VERCODE")).thenReturn("abc1");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(loginInfo));

            Field focusAccountField = AuthServiceImpl.class.getDeclaredField("focusAccount");
            focusAccountField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<String> focusAccount = (java.util.Set<String>) focusAccountField.get(null);
            focusAccount.add("user1");

            OperationResult result = authService.login(request, session);

            assertFalse(result.isSuccess());
            assertEquals("needsubmitvercode", result.getCode());
            verify(session, times(1)).removeAttribute("VERCODE");
        }
    }

    @Test
    void testGetVerificationCode_VCClose() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            authService.getVerificationCode(request, response, session);

            verify(response, times(1)).sendError(404);
        }
    }

    @Test
    void testGetVerificationCode_IOException() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Standard);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(response.getOutputStream()).thenThrow(new java.io.IOException("io error"));

            authService.getVerificationCode(request, response, session);

            verify(response, times(1)).sendError(500);
        }
    }

    @Test
    void testChangePassword_NotAllowed() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);
            when(reader.isAllowChangePassword()).thenReturn(false);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            OperationResult result = authService.changePassword(request);

            assertFalse(result.isSuccess());
            assertEquals("illegal", result.getCode());
        }
    }

    @Test
    void testChangePassword_NotLoggedIn() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);
            when(reader.isAllowChangePassword()).thenReturn(true);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);

            OperationResult result = authService.changePassword(request);

            assertFalse(result.isSuccess());
            assertEquals("mustlogin", result.getCode());
        }
    }

    @Test
    void testChangePassword_Success() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowChangePassword()).thenReturn(true);
            when(reader.checkAccountPwd(eq("user1"), eq("oldpwd"))).thenReturn(true);
            when(reader.changePassword(eq("user1"), eq("newpwd123"))).thenReturn(true);

            ChangePasswordInfoPojo info = new ChangePasswordInfoPojo();
            info.setOldPwd("oldpwd");
            info.setNewPwd("newpwd123");
            info.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.changePassword(request);

            assertTrue(result.isSuccess());
            assertEquals("success", result.getCode());
            verify(logUtil, times(1)).writeChangePasswordEvent(request, "user1");
        }
    }

    @Test
    void testChangePassword_OldPwdError() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowChangePassword()).thenReturn(true);
            when(reader.checkAccountPwd(eq("user1"), eq("wrongoldpwd"))).thenReturn(false);

            ChangePasswordInfoPojo info = new ChangePasswordInfoPojo();
            info.setOldPwd("wrongoldpwd");
            info.setNewPwd("newpwd123");
            info.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.changePassword(request);

            assertFalse(result.isSuccess());
            assertEquals("oldpwderror", result.getCode());
        }
    }

    @Test
    void testChangePassword_InvalidNewPwd() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowChangePassword()).thenReturn(true);
            when(reader.checkAccountPwd(eq("user1"), eq("oldpwd"))).thenReturn(true);

            ChangePasswordInfoPojo info = new ChangePasswordInfoPojo();
            info.setOldPwd("oldpwd");
            info.setNewPwd("ab");
            info.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.changePassword(request);

            assertFalse(result.isSuccess());
            assertEquals("invalidnewpwd", result.getCode());
        }
    }

    @Test
    void testChangePassword_Timeout() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowChangePassword()).thenReturn(true);

            ChangePasswordInfoPojo info = new ChangePasswordInfoPojo();
            info.setOldPwd("oldpwd");
            info.setNewPwd("newpwd123");
            info.setTime(String.valueOf(System.currentTimeMillis() - 60000L));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.changePassword(request);

            assertFalse(result.isSuccess());
            assertEquals("error", result.getCode());
        }
    }

    @Test
    void testChangePassword_Exception() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowChangePassword()).thenReturn(true);

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("user1");
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenThrow(new RuntimeException("decrypt error"));

            OperationResult result = authService.changePassword(request);

            assertFalse(result.isSuccess());
            assertEquals("cannotchangepwd", result.getCode());
            verify(logUtil, times(1)).writeException(any(Exception.class));
        }
    }

    @Test
    void testSignUp_NotAllowed() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);
            when(reader.isAllowSignUp()).thenReturn(false);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("illegal", result.getCode());
        }
    }

    @Test
    void testSignUp_AlreadyLoggedIn() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);
            when(reader.isAllowSignUp()).thenReturn(true);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn("existingUser");

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("mustlogout", result.getCode());
        }
    }

    @Test
    void testSignUp_Success() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);
            when(reader.foundAccount("newuser")).thenReturn(false);
            when(reader.createNewAccount(eq("newuser"), eq("password123"))).thenReturn(true);

            SignUpInfoPojo info = new SignUpInfoPojo();
            info.setAccount("newuser");
            info.setPwd("password123");
            info.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(request.getParameter("vercode")).thenReturn(null);
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));
            when(request.getSession(anyBoolean())).thenReturn(session);

            OperationResult result = authService.signUp(request);

            assertTrue(result.isSuccess());
            assertEquals("success", result.getCode());
            verify(logUtil, times(1)).writeSignUpEvent(request, "newuser");
        }
    }

    @Test
    void testSignUp_AccountExists() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);
            when(reader.foundAccount("existinguser")).thenReturn(true);

            SignUpInfoPojo info = new SignUpInfoPojo();
            info.setAccount("existinguser");
            info.setPwd("password123");
            info.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("accountexists", result.getCode());
        }
    }

    @Test
    void testSignUp_InvalidAccount() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);
            when(reader.foundAccount("ab")).thenReturn(false);

            SignUpInfoPojo info = new SignUpInfoPojo();
            info.setAccount("ab");
            info.setPwd("password123");
            info.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("invalidaccount", result.getCode());
        }
    }

    @Test
    void testSignUp_IllegalAccount() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);
            when(reader.foundAccount("user=name")).thenReturn(false);

            SignUpInfoPojo info = new SignUpInfoPojo();
            info.setAccount("user=name");
            info.setPwd("password123");
            info.setTime(String.valueOf(System.currentTimeMillis()));
            Gson gsonNoEscape = new GsonBuilder().disableHtmlEscaping().create();
            String jsonStr = gsonNoEscape.toJson(info);

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(jsonStr);

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("invalidaccount", result.getCode());
        }
    }

    @Test
    void testSignUp_InvalidPwd() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);
            when(reader.foundAccount("newuser")).thenReturn(false);

            SignUpInfoPojo info = new SignUpInfoPojo();
            info.setAccount("newuser");
            info.setPwd("ab");
            info.setTime(String.valueOf(System.currentTimeMillis()));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("invalidpwd", result.getCode());
        }
    }

    @Test
    void testSignUp_NeedVCode() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Standard);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("vercode")).thenReturn("wrong");
            when(session.getAttribute("VERCODE")).thenReturn("abc1");

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("needvercode", result.getCode());
        }
    }

    @Test
    void testSignUp_Exception() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenThrow(new RuntimeException("decrypt error"));

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("cannotsignup", result.getCode());
            verify(logUtil, times(1)).writeException(any(Exception.class));
        }
    }

    @Test
    void testSignUp_Timeout() throws Exception {
        try (MockedStatic<ConfigurationManager> crMock = mockStatic(ConfigurationManager.class);
             MockedStatic<RSADecryptUtil> rsaMock = mockStatic(RSADecryptUtil.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            crMock.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.getVCLevel()).thenReturn(VCLevel.Close);

            AuthServiceImpl authService = new AuthServiceImpl(rsaKeyUtil, logUtil, gson, ipAddrGetter);

            when(reader.isAllowSignUp()).thenReturn(true);
            when(reader.foundAccount("newuser")).thenReturn(false);

            SignUpInfoPojo info = new SignUpInfoPojo();
            info.setAccount("newuser");
            info.setPwd("password123");
            info.setTime(String.valueOf(System.currentTimeMillis() - 60000L));

            when(request.getSession()).thenReturn(session);
            when(session.getAttribute("ACCOUNT")).thenReturn(null);
            when(request.getParameter("encrypted")).thenReturn("encryptedData");
            when(rsaKeyUtil.getPrivateKey()).thenReturn("privateKey");
            rsaMock.when(() -> RSADecryptUtil.dncryption("encryptedData", "privateKey")).thenReturn(gson.toJson(info));

            OperationResult result = authService.signUp(request);

            assertFalse(result.isSuccess());
            assertEquals("error", result.getCode());
        }
    }
}
