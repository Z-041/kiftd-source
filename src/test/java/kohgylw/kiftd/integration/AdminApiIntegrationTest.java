package kohgylw.kiftd.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import kohgylw.kiftd.newcore.KiftdApplication;
import kohgylw.kiftd.printer.Printer;

/**
 * Web 账户管理 API（"/api/admin/accounts"）集成验证：
 * 认证复用 ApiAuthFilter（未登录 401 / 非管理员 403 / 管理员放行），
 * 覆盖账户列表只读端点（避免在真实配置上写入）。
 * 服务器配置与存储概览等系统管理已收敛至桌面端 GUI，不再暴露于 Web。
 */
@SpringBootTest(classes = { KiftdApplication.class, TestConfig.class })
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AdminApiIntegrationTest {

	static {
		if (Printer.instance == null) {
			Printer.init(false);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unauthenticatedAdminApi_returns401Json() throws Exception {
		mockMvc.perform(get("/api/admin/accounts"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void nonAdminSession_returns403Json() throws Exception {
		mockMvc.perform(get("/api/admin/accounts").sessionAttr("ACCOUNT", "normalUser"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void adminSession_accountsReturnsWrappedList() throws Exception {
		mockMvc.perform(get("/api/admin/accounts").sessionAttr("ACCOUNT", "admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void loggedInUser_accountInfoReturnsSuperAdminFlag() throws Exception {
		mockMvc.perform(get("/homeController/getAccountInfo.ajax").sessionAttr("ACCOUNT", "admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.account").value("admin"))
				.andExpect(jsonPath("$.superAdmin").value(true));
	}

	@Test
	void anonymous_accountInfoReturnsNotSuperAdmin() throws Exception {
		mockMvc.perform(get("/homeController/getAccountInfo.ajax"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.superAdmin").value(false));
	}
}
