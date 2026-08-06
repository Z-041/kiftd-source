package kohgylw.kiftd.newcore.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.newcore.domain.ApiResponse;
import kohgylw.kiftd.newcore.service.SystemHealthService;
import kohgylw.kiftd.server.util.ConfigurationManager;

@ExtendWith(MockitoExtension.class)
class SystemInfoControllerTest {

	@Mock
	private ConfigurationManager configurationManager;
	@Mock
	private SystemHealthService systemHealthService;

	private SystemInfoController controller;

	@BeforeEach
	void setUp() {
		controller = new SystemInfoController(configurationManager, systemHealthService);
	}

	@Test
	void testGetSystemInfo_wrapsWithApiResponse() {
		when(configurationManager.getPort()).thenReturn(8080);
		when(configurationManager.isHttpsEnabled()).thenReturn(false);
		when(configurationManager.getFileSystemPath()).thenReturn("/data/filesystem");

		ApiResponse<Map<String, Object>> response = controller.getSystemInfo();

		assertTrue(response.isSuccess());
		assertEquals("SUCCESS", response.getCode());
		assertEquals("kiftd", response.getData().get("appName"));
		assertEquals("1.3.0", response.getData().get("version"));
		assertEquals(8080, response.getData().get("port"));
		assertEquals(false, response.getData().get("httpsEnabled"));
		assertEquals("/data/filesystem", response.getData().get("fileSystemPath"));
	}

	@Test
	void testGetSystemInfo_httpsPortIncludedWhenEnabled() {
		when(configurationManager.getPort()).thenReturn(8080);
		when(configurationManager.isHttpsEnabled()).thenReturn(true);
		when(configurationManager.getHttpsPort()).thenReturn(8443);
		when(configurationManager.getFileSystemPath()).thenReturn("/data/filesystem");

		ApiResponse<Map<String, Object>> response = controller.getSystemInfo();

		assertTrue(response.isSuccess());
		assertEquals(8443, response.getData().get("httpsPort"));
	}

	@Test
	void testHealth_wrapsWithApiResponse() {
		Map<String, Object> health = new HashMap<>();
		health.put("status", "UP");
		when(systemHealthService.getHealthStatus()).thenReturn(health);

		ApiResponse<Map<String, Object>> response = controller.health();

		assertTrue(response.isSuccess());
		assertEquals("UP", response.getData().get("status"));
	}

	@Test
	void testMetrics_wrapsWithApiResponse() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("threadCount", 12);
		when(systemHealthService.getMetrics()).thenReturn(metrics);

		ApiResponse<Map<String, Object>> response = controller.metrics();

		assertTrue(response.isSuccess());
		assertEquals(12, response.getData().get("threadCount"));
	}

	@Test
	void testGetSystemStats_wrapsWithApiResponse() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("threadCount", 8);
		when(systemHealthService.getMetrics()).thenReturn(metrics);

		ApiResponse<Map<String, Object>> response = controller.getSystemStats();

		assertTrue(response.isSuccess());
		assertEquals(8, response.getData().get("threadCount"));
	}
}
