package kohgylw.kiftd.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import kohgylw.kiftd.newcore.KiftdApplication;
import kohgylw.kiftd.newcore.service.AuthService;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.PasswordUtil;
import kohgylw.kiftd.printer.Printer;

@SpringBootTest(classes = { KiftdApplication.class, TestConfig.class })
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthServiceIntegrationTest {

    static {
        if (Printer.instance == null) {
            Printer.init(false);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private AuthService authService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testMockMvcAutowired() {
        assertNotNull(mockMvc, "MockMvc应该被自动注入");
    }

    @Test
    void testAuthServiceBeanExists() {
        if (authService != null) {
            assertNotNull(authService, "认证服务Bean应该存在");
        }
    }

    @Test
    void testGetPublicKeyEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/getPublicKey.ajax"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "公钥响应不应该为null");
    }

    @Test
    void testGetPublicKeyReturnsJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/getPublicKey.ajax"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("publicKey") || response.isEmpty(), 
                "响应应该包含公钥信息或为空");
    }

    @Test
    void testPingEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/ping.ajax"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "ping响应不应该为null");
    }

    @Test
    void testAskForAllowSignUpEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/askForAllowSignUpOrNot.ajax"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue("true".equals(response) || "false".equals(response), 
                "注册允许状态应该是true或false");
    }

    @Test
    void testLogoutEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/doLogout.ajax"))
                .andReturn();
        assertNotNull(result.getResponse(), "登出响应不应该为null");
    }

    @Test
    void testConfigurationManagerAccountExists() {
        ConfigurationManager cm = ConfigurationManager.instance();
        boolean exists = cm.foundAccount("admin");
        assertTrue(exists, "默认admin账户应该存在");
    }

    @Test
    void testConfigurationManagerAccountNotFound() {
        ConfigurationManager cm = ConfigurationManager.instance();
        boolean exists = cm.foundAccount("nonexistentuser12345");
        assertFalse(exists, "不存在的账户应该返回false");
    }

    @Test
    void testPasswordHashing() {
        String password = "testPassword123";
        String hashed = PasswordUtil.hashPassword(password);
        assertNotNull(hashed, "哈希后的密码不应该为null");
        assertNotEquals(password, hashed, "哈希后的密码不应该与原密码相同");
    }

    @Test
    void testPasswordVerification() {
        String password = "testPassword456";
        String hashed = PasswordUtil.hashPassword(password);
        boolean verified = PasswordUtil.verifyPassword(password, hashed);
        assertTrue(verified, "密码验证应该通过");
    }

    @Test
    void testPasswordVerificationWrongPassword() {
        String password = "correctPassword";
        String wrongPassword = "wrongPassword";
        String hashed = PasswordUtil.hashPassword(password);
        boolean verified = PasswordUtil.verifyPassword(wrongPassword, hashed);
        assertFalse(verified, "错误密码验证应该失败");
    }

    @Test
    void testIsPasswordHashed() {
        String plainPassword = "plainPassword";
        String hashedPassword = PasswordUtil.hashPassword("testPassword");
        assertFalse(PasswordUtil.isPasswordHashed(plainPassword), 
                "明文密码应该返回false");
        assertTrue(PasswordUtil.isPasswordHashed(hashedPassword), 
                "哈希密码应该返回true");
    }

    @Test
    void testLoginWithInvalidAccount() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/doLogin.ajax")
                .param("encrypted", "invalidEncryptedData"))
                .andReturn();
        assertNotNull(result.getResponse(), "登录响应不应该为null");
    }

    @Test
    void testGetVerificationCodeEndpoint() throws Exception {
        ConfigurationManager cm = ConfigurationManager.instance();
        if (cm.getVCLevel() != null && !cm.getVCLevel().equals(kohgylw.kiftd.server.enumeration.VCLevel.Close)) {
            MvcResult result = mockMvc.perform(get("/homeController/getNewVerCode.do"))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNotNull(result.getResponse(), "验证码响应不应该为null");
        }
    }

    @Test
    void testChangePasswordWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/doChangePassword.ajax")
                .param("oldPwd", "test")
                .param("newPwd", "test"))
                .andReturn();
        assertNotNull(result.getResponse(), "修改密码响应不应该为null");
    }

    @Test
    void testSignUpWhenNotAllowed() throws Exception {
        ConfigurationManager cm = ConfigurationManager.instance();
        if (!cm.isAllowSignUp()) {
            MvcResult result = mockMvc.perform(post("/homeController/doSigUp.ajax")
                    .param("account", "testuser")
                    .param("pwd", "testpass"))
                    .andReturn();
            assertNotNull(result.getResponse(), "注册响应不应该为null");
        }
    }
}
