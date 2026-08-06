package kohgylw.kiftd.newcore.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemHealthServiceTest {

	@Mock
	private DataSource dataSource;
	@Mock
	private ConfigurationManager configurationManager;
	@Mock
	private Connection connection;

	@TempDir
	File tempDir;

	private SystemHealthService service;

	@BeforeEach
	void setUp() throws Exception {
		Printer.instance = mock(Printer.class);
		service = new SystemHealthService(dataSource, configurationManager);
	}

	@AfterEach
	void tearDown() {
		Printer.instance = null;
	}

	@Test
	void testGetHealthStatus_AllComponentsOk_ReturnsUp() throws Exception {
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.isValid(3)).thenReturn(true);
		when(configurationManager.getFileSystemPath()).thenReturn(tempDir.getAbsolutePath());

		Map<String, Object> health = service.getHealthStatus();

		assertEquals("UP", health.get("status"));
		assertEquals(true, ((Map<?, ?>) health.get("database")).get("status"));
		assertEquals(true, ((Map<?, ?>) health.get("fileSystem")).get("status"));
		assertEquals(true, ((Map<?, ?>) health.get("disk")).get("status"));
		assertEquals("kiftd", health.get("appName"));
		assertEquals("1.3.0", health.get("version"));
		assertNotNull(health.get("memory"));
		assertNotNull(health.get("uptime"));
	}

	@Test
	void testGetHealthStatus_DatabaseUnavailable_ReturnsDown() throws Exception {
		when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));
		when(configurationManager.getFileSystemPath()).thenReturn(tempDir.getAbsolutePath());

		Map<String, Object> health = service.getHealthStatus();

		assertEquals("DOWN", health.get("status"));
		assertEquals(false, ((Map<?, ?>) health.get("database")).get("status"));
	}

	@Test
	void testGetHealthStatus_FileSystemPathMissing_ReturnsDown() throws Exception {
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.isValid(3)).thenReturn(true);
		String missingPath = tempDir.getAbsolutePath() + File.separator + "does-not-exist";
		when(configurationManager.getFileSystemPath()).thenReturn(missingPath);

		Map<String, Object> health = service.getHealthStatus();

		assertEquals("DOWN", health.get("status"));
		assertEquals(false, ((Map<?, ?>) health.get("fileSystem")).get("status"));
		assertEquals(false, ((Map<?, ?>) health.get("disk")).get("status"));
	}

	@Test
	void testGetMetrics_ReturnsExpectedSections() {
		when(configurationManager.getFileSystemPath()).thenReturn(tempDir.getAbsolutePath());

		Map<String, Object> metrics = service.getMetrics();

		assertTrue(metrics.containsKey("heapMemory"));
		assertTrue(metrics.containsKey("nonHeapMemory"));
		assertTrue(metrics.containsKey("threadCount"));
		assertTrue(metrics.containsKey("disk"));
		assertTrue(metrics.containsKey("uptimeMs"));
		assertTrue(metrics.containsKey("uptime"));
		assertTrue(metrics.containsKey("requestStats"));
		assertTrue(metrics.containsKey("exceptionStats"));
		assertNotNull(metrics.get("availableProcessors"));
	}

	@Test
	void testGetMetrics_DiskPathMissing_DiskSectionEmpty() {
		String missingPath = tempDir.getAbsolutePath() + File.separator + "nope";
		when(configurationManager.getFileSystemPath()).thenReturn(missingPath);

		Map<String, Object> metrics = service.getMetrics();

		Map<?, ?> disk = (Map<?, ?>) metrics.get("disk");
		assertTrue(disk.isEmpty());
	}
}
