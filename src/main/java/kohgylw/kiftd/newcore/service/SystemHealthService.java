package kohgylw.kiftd.newcore.service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.controller.GlobalExceptionHandler;
import kohgylw.kiftd.newcore.infrastructure.logging.ApiPerformanceFilter;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;


@Service
public class SystemHealthService {

	private final DataSource dataSource;
	private final ConfigurationManager configurationManager;

	public SystemHealthService(DataSource dataSource, ConfigurationManager configurationManager) {
		this.dataSource = dataSource;
		this.configurationManager = configurationManager;
	}

	public Map<String, Object> getHealthStatus() {
		Map<String, Object> health = new HashMap<>();
		boolean overallStatus = true;

		Map<String, Object> database = checkDatabase();
		health.put("database", database);
		overallStatus &= (boolean) database.get("status");

		Map<String, Object> fileSystem = checkFileSystem();
		health.put("fileSystem", fileSystem);
		overallStatus &= (boolean) fileSystem.get("status");

		Map<String, Object> memory = checkMemory();
		health.put("memory", memory);
		overallStatus &= (boolean) memory.get("status");

		Map<String, Object> disk = checkDiskSpace();
		health.put("disk", disk);
		overallStatus &= (boolean) disk.get("status");

		Map<String, Object> uptime = getUptimeInfo();
		health.put("uptime", uptime);

		health.put("status", overallStatus ? "UP" : "DOWN");
		health.put("appName", "kiftd");
		health.put("version", "1.3.0");

		return health;
	}

	public Map<String, Object> getMetrics() {
		Map<String, Object> metrics = new HashMap<>();

		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		Map<String, Object> heap = new HashMap<>();
		heap.put("used", memoryBean.getHeapMemoryUsage().getUsed());
		heap.put("committed", memoryBean.getHeapMemoryUsage().getCommitted());
		heap.put("max", memoryBean.getHeapMemoryUsage().getMax());
		heap.put("usagePercent", calculatePercent(memoryBean.getHeapMemoryUsage().getUsed(),
				memoryBean.getHeapMemoryUsage().getMax()));
		metrics.put("heapMemory", heap);

		Map<String, Object> nonHeap = new HashMap<>();
		nonHeap.put("used", memoryBean.getNonHeapMemoryUsage().getUsed());
		nonHeap.put("committed", memoryBean.getNonHeapMemoryUsage().getCommitted());
		metrics.put("nonHeapMemory", nonHeap);

		metrics.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
		metrics.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());

		Map<String, Object> disk = getDiskMetrics();
		metrics.put("disk", disk);

		long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
		metrics.put("uptimeMs", uptimeMs);
		metrics.put("uptime", formatUptime(uptimeMs));

		metrics.put("availableProcessors", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
		metrics.put("systemLoadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());

		Map<String, Object> requestStats = new HashMap<>();
		requestStats.put("totalRequestCount", ApiPerformanceFilter.getTotalRequestCount());
		requestStats.put("slowRequestCount", ApiPerformanceFilter.getSlowRequestCount());
		requestStats.put("averageResponseTimeMs", ApiPerformanceFilter.getAverageResponseTime());
		requestStats.put("minResponseTimeMs", ApiPerformanceFilter.getMinResponseTime());
		requestStats.put("maxResponseTimeMs", ApiPerformanceFilter.getMaxResponseTime());
		requestStats.put("endpointRequestCount", ApiPerformanceFilter.getEndpointRequestCount());
		metrics.put("requestStats", requestStats);

		Map<String, Object> exceptionStats = new HashMap<>();
		long totalExceptions = GlobalExceptionHandler.getTotalExceptionCount();
		long totalRequests = ApiPerformanceFilter.getTotalRequestCount();
		double exceptionRate = totalRequests > 0 ? (double) totalExceptions / totalRequests * 100.0 : 0.0;
		exceptionStats.put("totalExceptionCount", totalExceptions);
		exceptionStats.put("exceptionRatePercent", exceptionRate);
		exceptionStats.put("exceptionTypeCount", GlobalExceptionHandler.getExceptionTypeCount());
		metrics.put("exceptionStats", exceptionStats);

		return metrics;
	}

	private Map<String, Object> checkDatabase() {
		Map<String, Object> result = new HashMap<>();
		try (Connection conn = dataSource.getConnection()) {
			boolean valid = conn.isValid(3);
			result.put("status", valid);
			result.put("message", valid ? "数据库连接正常" : "数据库连接验证失败");
		} catch (SQLException e) {
			result.put("status", false);
			result.put("message", "数据库连接异常: " + e.getMessage());
			Printer.instance.print("[健康检查] 数据库连接检查失败: " + e.getMessage());
		}
		return result;
	}

	private Map<String, Object> checkFileSystem() {
		Map<String, Object> result = new HashMap<>();
		String fsPath = configurationManager.getFileSystemPath();
		File fsDir = new File(fsPath);

		if (!fsDir.isDirectory()) {
			result.put("status", false);
			result.put("message", "文件系统路径不存在: " + fsPath);
			return result;
		}

		if (!fsDir.canRead()) {
			result.put("status", false);
			result.put("message", "文件系统路径不可读: " + fsPath);
			return result;
		}

		if (!fsDir.canWrite()) {
			result.put("status", false);
			result.put("message", "文件系统路径不可写: " + fsPath);
			return result;
		}

		File testFile = new File(fsDir, ".health_check_" + System.currentTimeMillis());
		try {
			boolean created = testFile.createNewFile();
			if (created) {
				testFile.delete();
			}
			result.put("status", true);
			result.put("message", "文件系统读写正常");
			result.put("path", fsPath);
		} catch (IOException e) {
			result.put("status", false);
			result.put("message", "文件系统读写测试失败: " + e.getMessage());
			Printer.instance.print("[健康检查] 文件系统检查失败: " + e.getMessage());
		}
		return result;
	}

	private Map<String, Object> checkMemory() {
		Map<String, Object> result = new HashMap<>();
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		long used = memoryBean.getHeapMemoryUsage().getUsed();
		long max = memoryBean.getHeapMemoryUsage().getMax();
		double usagePercent = calculatePercent(used, max);

		result.put("used", used);
		result.put("max", max);
		result.put("usagePercent", usagePercent);

		if (usagePercent > 90) {
			result.put("status", false);
			result.put("message", "内存使用率过高: " + String.format("%.1f%%", usagePercent));
		} else {
			result.put("status", true);
			result.put("message", "内存使用正常");
		}
		return result;
	}

	private Map<String, Object> checkDiskSpace() {
		Map<String, Object> result = new HashMap<>();
		String fsPath = configurationManager.getFileSystemPath();
		File fsDir = new File(fsPath);

		if (!fsDir.isDirectory()) {
			result.put("status", false);
			result.put("message", "文件系统路径不存在: " + fsPath);
			return result;
		}

		long totalSpace = fsDir.getTotalSpace();
		long freeSpace = fsDir.getFreeSpace();
		long usedSpace = totalSpace - freeSpace;
		double usagePercent = calculatePercent(usedSpace, totalSpace);

		result.put("totalSpace", totalSpace);
		result.put("freeSpace", freeSpace);
		result.put("usedSpace", usedSpace);
		result.put("usagePercent", usagePercent);

		if (usagePercent > 95) {
			result.put("status", false);
			result.put("message", "磁盘空间不足: 使用率 " + String.format("%.1f%%", usagePercent));
		} else {
			result.put("status", true);
			result.put("message", "磁盘空间充足");
		}
		return result;
	}

	private Map<String, Object> getUptimeInfo() {
		Map<String, Object> result = new HashMap<>();
		long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
		result.put("uptimeMs", uptimeMs);
		result.put("uptime", formatUptime(uptimeMs));
		result.put("startTime", ManagementFactory.getRuntimeMXBean().getStartTime());
		return result;
	}

	private Map<String, Object> getDiskMetrics() {
		Map<String, Object> disk = new HashMap<>();
		String fsPath = configurationManager.getFileSystemPath();
		File fsDir = new File(fsPath);

		if (fsDir.isDirectory()) {
			long totalSpace = fsDir.getTotalSpace();
			long freeSpace = fsDir.getFreeSpace();
			long usedSpace = totalSpace - freeSpace;
			disk.put("totalSpace", totalSpace);
			disk.put("freeSpace", freeSpace);
			disk.put("usedSpace", usedSpace);
			disk.put("usagePercent", calculatePercent(usedSpace, totalSpace));
		}
		return disk;
	}

	private double calculatePercent(long used, long total) {
		if (total <= 0) {
			return 0.0;
		}
		return (double) used / total * 100.0;
	}

	private String formatUptime(long uptimeMs) {
		long seconds = uptimeMs / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		if (days > 0) {
			return String.format("%d天 %d小时 %d分钟", days, hours % 24, minutes % 60);
		} else if (hours > 0) {
			return String.format("%d小时 %d分钟", hours, minutes % 60);
		} else if (minutes > 0) {
			return String.format("%d分钟 %d秒", minutes, seconds % 60);
		} else {
			return String.format("%d秒", seconds);
		}
	}
}
