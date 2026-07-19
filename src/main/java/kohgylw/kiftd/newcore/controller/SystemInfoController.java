package kohgylw.kiftd.newcore.controller;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.service.SystemHealthService;

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
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("appName", "kiftd");
        info.put("version", "1.2.3-SNAPSHOT");

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        info.put("uptime", formatUptime(uptimeMs));
        info.put("uptimeMs", uptimeMs);

        info.put("port", configurationManager.getPort());
        info.put("httpsEnabled", configurationManager.isHttpsEnabled());
        if (configurationManager.isHttpsEnabled()) {
            info.put("httpsPort", configurationManager.getHttpsPort());
        }

        info.put("fileSystemPath", configurationManager.getFileSystemPath());

        return info;
    }

    @GetMapping("/stats")
    public Map<String, Object> getSystemStats() {
        Map<String, Object> metrics = systemHealthService.getMetrics();
        Map<String, Object> stats = new HashMap<>();

        stats.put("heapMemory", metrics.get("heapMemory"));
        stats.put("nonHeapMemory", metrics.get("nonHeapMemory"));

        stats.put("osName", ManagementFactory.getOperatingSystemMXBean().getName());
        stats.put("osVersion", ManagementFactory.getOperatingSystemMXBean().getVersion());
        stats.put("availableProcessors", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
        stats.put("systemLoadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());

        stats.put("threadCount", metrics.get("threadCount"));

        return stats;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return systemHealthService.getHealthStatus();
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return systemHealthService.getMetrics();
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
