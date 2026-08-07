package kohgylw.kiftd.newcore.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import kohgylw.kiftd.newcore.controller.AccountAdminController.AccountCreateRequest;
import kohgylw.kiftd.newcore.controller.AccountAdminController.AuthRequest;
import kohgylw.kiftd.newcore.controller.AccountAdminController.PasswordRequest;
import kohgylw.kiftd.newcore.domain.ApiResponse;
import kohgylw.kiftd.server.util.ConfigurationManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountAdminControllerTest {

	@Mock
	private ConfigurationManager configurationManager;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpSession session;

	private AccountAdminController controller;

	@BeforeEach
	void setUp() {
		controller = new AccountAdminController(configurationManager);
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute("ACCOUNT")).thenReturn("admin");
	}

	// ==================== 账户列表 ====================

	@Test
	void testListAccounts_wrapsWithApiResponse() {
		when(configurationManager.getAllAccounts()).thenReturn(List.of("admin", "user1"));
		when(configurationManager.getAccountAuth("admin")).thenReturn("cudrml");
		when(configurationManager.getAccountGroup("admin")).thenReturn("admin");
		when(configurationManager.isSuperAdmin("admin")).thenReturn(true);
		when(configurationManager.getUploadFileSize("admin")).thenReturn(0L);
		when(configurationManager.getDownloadMaxRate("admin")).thenReturn(0L);
		when(configurationManager.getAccountAuth("user1")).thenReturn("ul");
		when(configurationManager.isSuperAdmin("user1")).thenReturn(false);
		when(configurationManager.getUploadFileSize("user1")).thenReturn(1024L * 1024);
		when(configurationManager.getDownloadMaxRate("user1")).thenReturn(512000L);

		ApiResponse<List<Map<String, Object>>> response = controller.listAccounts();

		assertTrue(response.isSuccess());
		assertEquals(2, response.getData().size());
		Map<String, Object> admin = response.getData().get(0);
		assertEquals("admin", admin.get("account"));
		assertEquals(Boolean.TRUE, admin.get("superAdmin"));
		Map<String, Object> user1 = response.getData().get(1);
		assertEquals("user1", user1.get("account"));
		assertEquals(Boolean.FALSE, user1.get("superAdmin"));
		assertEquals(1024L * 1024, user1.get("uploadMaxSize"));
		assertEquals(512000L, user1.get("downloadMaxRate"));
	}

	// ==================== 创建账户 ====================

	@Test
	void testCreateAccount_success_returnsSuccess() throws Exception {
		when(configurationManager.createNewAccount("user2", "pass123")).thenReturn(true);

		ApiResponse<Void> response = controller.createAccount(new AccountCreateRequest("user2", "pass123", "ul"));

		assertTrue(response.isSuccess());
		verify(configurationManager).updateAccountAuth("user2", "ul");
	}

	@Test
	void testCreateAccount_blankAccount_returnsBadRequest() throws Exception {
		ApiResponse<Void> response = controller.createAccount(new AccountCreateRequest("  ", "pass", "ul"));

		assertFalse(response.isSuccess());
		assertEquals("BAD_REQUEST", response.getCode());
	}

	@Test
	void testCreateAccount_invalidAuth_returnsBadRequest() throws Exception {
		ApiResponse<Void> response = controller.createAccount(new AccountCreateRequest("user2", "pass", "xyz"));

		assertFalse(response.isSuccess());
		assertEquals("BAD_REQUEST", response.getCode());
		verify(configurationManager, never()).createNewAccount(anyString(), anyString());
	}

	@Test
	void testCreateAccount_alreadyExists_returnsUserAlreadyExists() throws Exception {
		when(configurationManager.createNewAccount("user2", "pass")).thenReturn(false);

		ApiResponse<Void> response = controller.createAccount(new AccountCreateRequest("user2", "pass", ""));

		assertFalse(response.isSuccess());
		assertEquals("USER_ALREADY_EXISTS", response.getCode());
	}

	// ==================== 删除账户 ====================

	@Test
	void testDeleteAccount_builtInAdmin_forbidden() throws Exception {
		ApiResponse<Void> response = controller.deleteAccount("admin", request);

		assertFalse(response.isSuccess());
		assertEquals("FORBIDDEN", response.getCode());
		verify(configurationManager, never()).deleteAccount(anyString());
	}

	@Test
	void testDeleteAccount_currentSessionAccount_forbidden() throws Exception {
		// 当前登录账户为 root 时，删除自身应被拒绝
		when(session.getAttribute("ACCOUNT")).thenReturn("root");

		ApiResponse<Void> response = controller.deleteAccount("root", request);

		assertFalse(response.isSuccess());
		assertEquals("FORBIDDEN", response.getCode());
		verify(configurationManager, never()).deleteAccount(anyString());
	}

	@Test
	void testDeleteAccount_success() throws Exception {
		when(configurationManager.deleteAccount("user1")).thenReturn(true);

		ApiResponse<Void> response = controller.deleteAccount("user1", request);

		assertTrue(response.isSuccess());
	}

	@Test
	void testDeleteAccount_notFound() throws Exception {
		when(configurationManager.deleteAccount("ghost")).thenReturn(false);

		ApiResponse<Void> response = controller.deleteAccount("ghost", request);

		assertFalse(response.isSuccess());
		assertEquals("USER_NOT_FOUND", response.getCode());
	}

	// ==================== 重置密码 / 修改权限 ====================

	@Test
	void testResetPassword_success() throws Exception {
		when(configurationManager.resetPassword("user1", "newpass")).thenReturn(true);

		ApiResponse<Void> response = controller.resetPassword("user1", new PasswordRequest("newpass"));

		assertTrue(response.isSuccess());
	}

	@Test
	void testResetPassword_emptyPassword_returnsBadRequest() throws Exception {
		ApiResponse<Void> response = controller.resetPassword("user1", new PasswordRequest(""));

		assertFalse(response.isSuccess());
		assertEquals("BAD_REQUEST", response.getCode());
		verify(configurationManager, never()).resetPassword(anyString(), anyString());
	}

	@Test
	void testUpdateAuth_success() throws Exception {
		when(configurationManager.updateAccountAuth("user1", "cudrml")).thenReturn(true);

		ApiResponse<Void> response = controller.updateAuth("user1", new AuthRequest("cudrml"));

		assertTrue(response.isSuccess());
	}

	@Test
	void testUpdateAuth_invalidAuth_returnsBadRequest() throws Exception {
		ApiResponse<Void> response = controller.updateAuth("user1", new AuthRequest("cux"));

		assertFalse(response.isSuccess());
		assertEquals("BAD_REQUEST", response.getCode());
	}
}
