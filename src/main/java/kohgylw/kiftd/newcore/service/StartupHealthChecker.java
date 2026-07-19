package kohgylw.kiftd.newcore.service;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.printer.Printer;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;

@Component
public class StartupHealthChecker {

	private final ConfigurationManager configurationManager;

	public StartupHealthChecker(ConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
	}

	public boolean performHealthCheck(ApplicationContext context) {
		Printer.instance.print("正在执行启动健康检查...");
		boolean allPassed = true;

		if (!checkDatabaseConnection(context)) {
			allPassed = false;
			Printer.instance.print("健康检查失败：数据库连接不可用。");
		} else {
			Printer.instance.print("健康检查通过：数据库连接正常。");
		}

		if (!checkFileSystemAccess()) {
			allPassed = false;
			Printer.instance.print("健康检查失败：文件系统不可访问。");
		} else {
			Printer.instance.print("健康检查通过：文件系统可访问。");
		}

		return allPassed;
	}

	private boolean checkDatabaseConnection(ApplicationContext context) {
		try {
			if (context != null) {
				DataSource dataSource = context.getBean(DataSource.class);
				if (dataSource != null) {
					try (Connection conn = dataSource.getConnection()) {
						return conn.isValid(3);
					}
				}
			}
		} catch (Exception e) {
			Printer.instance.print("数据库连接检查失败: " + e.getMessage());
		}
		return false;
	}

	private boolean checkFileSystemAccess() {
		try {
			String fsPath = configurationManager.getFileSystemPath();
			File fsDir = new File(fsPath);

			if (!fsDir.isDirectory()) {
				Printer.instance.print("文件系统路径不存在: " + fsPath);
				return false;
			}

			if (!fsDir.canRead()) {
				Printer.instance.print("文件系统路径不可读: " + fsPath);
				return false;
			}

			if (!fsDir.canWrite()) {
				Printer.instance.print("文件系统路径不可写: " + fsPath);
				return false;
			}

			File testFile = new File(fsDir, ".startup_health_check_" + System.currentTimeMillis());
			try {
				boolean created = testFile.createNewFile();
				if (created) {
					testFile.delete();
				}
				return true;
			} catch (Exception e) {
				Printer.instance.print("文件系统读写测试失败: " + e.getMessage());
				return false;
			}
		} catch (Exception e) {
			Printer.instance.print("文件系统检查失败: " + e.getMessage());
			return false;
		}
	}
}
