package kohgylw.kiftd.newcore.controller;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kohgylw.kiftd.newcore.domain.ApiResponse;
import kohgylw.kiftd.newcore.service.SystemHealthService;
import kohgylw.kiftd.server.util.ConfigurationManager;

/**
 *
 * <h2>系统信息 API 控制器</h2>
 * <p>
 * 程序化访问通道（"/api/*"）的系统信息端点，响应统一使用 ApiResponse 包装；
 * 管理员认证由 ApiAuthFilter 统一执行，本控制器不再内嵌鉴权逻辑。
 * </p>
 *
 * @author 技术债治理迭代
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

	private final ConfigurationManager configurationManager;
	private final SystemHealthService systemHealthService;

	public SystemInfoController(ConfigurationManager configurationManager, SystemHealthService systemHealthService) {
		this.configurationManager = configurationManager;
		this.systemHealthService = systemHealthService;
	}

	@GetMapping("/info")
	public ApiResponse<Map<String, Object>> getSystemInfo() {
		Map<String, Object> info = new HashMap<>();

		info.put("appName", "kiftd");
		info.put("version", "1.3.0");

		long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
		info.put("uptime", formatUptime(uptimeMs));
		info.put("uptimeMs", uptimeMs);

		info.put("port", configurationManager.getPort());
		info.put("httpsEnabled", configurationManager.isHttpsEnabled());
		if (configurationManager.isHttpsEnabled()) {
			info.put("httpsPort", configurationManager.getHttpsPort());
		}

		info.put("fileSystemPath", configurationManager.getFileSystemPath());

		return ApiResponse.success(info);
	}

	@GetMapping("/stats")
	public ApiResponse<Map<String, Object>> getSystemStats() {
		Map<String, Object> metrics = systemHealthService.getMetrics();
		Map<String, Object> stats = new HashMap<>();

		stats.put("heapMemory", metrics.get("heapMemory"));
		stats.put("nonHeapMemory", metrics.get("nonHeapMemory"));

		stats.put("osName", ManagementFactory.getOperatingSystemMXBean().getName());
		stats.put("osVersion", ManagementFactory.getOperatingSystemMXBean().getVersion());
		stats.put("availableProcessors", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
		stats.put("systemLoadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());

		stats.put("threadCount", metrics.get("threadCount"));

		return ApiResponse.success(stats);
	}

	@GetMapping("/health")
	public ApiResponse<Map<String, Object>> health() {
		return ApiResponse.success(systemHealthService.getHealthStatus());
	}

	@GetMapping("/metrics")
	public ApiResponse<Map<String, Object>> metrics() {
		return ApiResponse.success(systemHealthService.getMetrics());
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
