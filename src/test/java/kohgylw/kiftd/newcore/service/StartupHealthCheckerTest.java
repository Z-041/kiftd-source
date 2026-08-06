package kohgylw.kiftd.newcore.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.sql.Connection;

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
import org.springframework.context.ApplicationContext;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StartupHealthCheckerTest {

	@Mock
	private ConfigurationManager configurationManager;
	@Mock
	private ApplicationContext context;
	@Mock
	private DataSource dataSource;
	@Mock
	private Connection connection;

	@TempDir
	File tempDir;

	private StartupHealthChecker checker;

	@BeforeEach
	void setUp() {
		Printer.instance = mock(Printer.class);
		checker = new StartupHealthChecker(configurationManager);
	}

	@AfterEach
	void tearDown() {
		Printer.instance = null;
	}

	@Test
	void testPerformHealthCheck_AllPassed() throws Exception {
		when(context.getBean(DataSource.class)).thenReturn(dataSource);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.isValid(3)).thenReturn(true);
		when(configurationManager.getFileSystemPath()).thenReturn(tempDir.getAbsolutePath());

		assertTrue(checker.performHealthCheck(context));
	}

	@Test
	void testPerformHealthCheck_DatabaseUnavailable() throws Exception {
		when(context.getBean(DataSource.class)).thenReturn(dataSource);
		when(dataSource.getConnection()).thenThrow(new RuntimeException("db down"));
		when(configurationManager.getFileSystemPath()).thenReturn(tempDir.getAbsolutePath());

		assertFalse(checker.performHealthCheck(context));
	}

	@Test
	void testPerformHealthCheck_FileSystemPathMissing() throws Exception {
		when(context.getBean(DataSource.class)).thenReturn(dataSource);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.isValid(3)).thenReturn(true);
		String missingPath = tempDir.getAbsolutePath() + File.separator + "missing";
		when(configurationManager.getFileSystemPath()).thenReturn(missingPath);

		assertFalse(checker.performHealthCheck(context));
	}

	@Test
	void testPerformHealthCheck_NullContext_FileSystemStillChecked() {
		when(configurationManager.getFileSystemPath()).thenReturn(tempDir.getAbsolutePath());

		// context 为 null → 数据库检查失败（返回 false），文件系统正常，整体仍为 false
		assertFalse(checker.performHealthCheck(null));
	}

	@Test
	void testPerformHealthCheck_DataSourceBeanMissing() {
		when(context.getBean(DataSource.class)).thenReturn(null);
		when(configurationManager.getFileSystemPath()).thenReturn(tempDir.getAbsolutePath());

		assertFalse(checker.performHealthCheck(context));
	}
}
