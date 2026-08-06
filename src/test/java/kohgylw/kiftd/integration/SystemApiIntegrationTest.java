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
 * API 通道（"/api/*"）集成验证（ARCH-001）：
 * 验证 ApiAuthFilter 在真实 Spring 容器中注册并生效——未登录 401 JSON、
 * 非管理员 403 JSON、管理员放行且响应为 ApiResponse 统一包装。
 */
@SpringBootTest(classes = { KiftdApplication.class, TestConfig.class })
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class SystemApiIntegrationTest {

	static {
		if (Printer.instance == null) {
			Printer.init(false);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unauthenticatedApiRequest_returns401Json() throws Exception {
		mockMvc.perform(get("/api/system/health"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void nonAdminSession_returns403Json() throws Exception {
		mockMvc.perform(get("/api/system/health").sessionAttr("ACCOUNT", "normalUser"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void adminSession_healthReturnsWrappedApiResponse() throws Exception {
		mockMvc.perform(get("/api/system/health").sessionAttr("ACCOUNT", "admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data.status").exists())
				.andExpect(jsonPath("$.data.version").value("1.3.0"));
	}

	@Test
	void adminSession_infoReturnsWrappedApiResponse() throws Exception {
		mockMvc.perform(get("/api/system/info").sessionAttr("ACCOUNT", "admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.appName").value("kiftd"))
				.andExpect(jsonPath("$.data.version").value("1.3.0"));
	}
}
