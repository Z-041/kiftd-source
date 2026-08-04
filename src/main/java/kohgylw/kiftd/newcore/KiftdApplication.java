package kohgylw.kiftd.newcore;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.newcore.config.DataSourceConfig;
import kohgylw.kiftd.newcore.config.UndertowServerConfig;
import kohgylw.kiftd.newcore.config.WebMvcConfig;
import kohgylw.kiftd.newcore.service.StartupHealthChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@SpringBootApplication
@Import({ WebMvcConfig.class, DataSourceConfig.class, ConfigurationManager.class, UndertowServerConfig.class })
public class KiftdApplication {

	private static volatile ApplicationContext context;
	private static volatile boolean running = false;
	private static volatile Thread shutdownHook;

	static {
		System.setProperty("logging.level.root", "ERROR");
	}

	public synchronized boolean start() {
		Printer.instance.print("正在初始化服务器设置...");
		if (!running) {
			ConfigurationManager.instance().revalidate();
			if (ConfigurationManager.instance().getStatus() == 0) {
				try {
					Printer.instance.print("正在开启服务器引擎...");
					SpringApplication app = new SpringApplication(KiftdApplication.class);
					app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
					context = app.run();
					running = (context != null);
					if (running) {
						registerShutdownHook();
						runHealthCheck();
						Printer.instance.print("服务器引擎已启动。");
					}
					return running;
				} catch (Exception e) {
					Printer.instance.print(e.toString());
					Printer.instance.print("出现错误，服务器引擎启动失败。");
					return false;
				}
			}
			Printer.instance.print("服务器设置检查失败，无法开启服务器。");
			return false;
		}
		Printer.instance.print("服务器正在运行中。");
		return true;
	}

	private void runHealthCheck() {
		try {
			StartupHealthChecker healthChecker = context.getBean(StartupHealthChecker.class);
			if (!healthChecker.performHealthCheck(context)) {
				Printer.instance.print("警告：启动健康检查未完全通过，但服务器仍将运行。");
			}
		} catch (Exception e) {
			Printer.instance.print("警告：健康检查服务不可用，跳过健康检查。");
		}
	}

	public synchronized boolean stop() {
		Printer.instance.print("正在关闭服务器...");
		removeShutdownHook();
		if (context != null) {
			Printer.instance.print("正在终止服务器引擎...");
			try {
				cleanupResources();
				int exitCode = SpringApplication.exit(context, () -> 0);
				running = false;
				context = null;
				Printer.instance.print("服务器引擎已终止。");
				return exitCode == 0;
			} catch (Exception e) {
				Printer.instance.print("出现错误，服务器引擎关闭失败：" + e.toString());
				return false;
			}
		}
		Printer.instance.print("服务器未启动。");
		return true;
	}

	private void registerShutdownHook() {
		if (shutdownHook == null) {
			synchronized (KiftdApplication.class) {
				if (shutdownHook == null) {
					shutdownHook = new Thread(() -> {
						Printer.instance.print("收到JVM关闭信号，正在优雅关闭服务器...");
						try {
							cleanupResources();
							if (context != null) {
								SpringApplication.exit(context, () -> 0);
								context = null;
							}
							running = false;
							Printer.instance.print("服务器已优雅关闭。");
						} catch (Exception e) {
							Printer.instance.print("优雅关闭时发生错误：" + e.getMessage());
						}
					}, "kiftd-shutdown-hook");
					Runtime.getRuntime().addShutdownHook(shutdownHook);
					Printer.instance.print("已注册优雅关闭钩子。");
				}
			}
		}
	}

	private void removeShutdownHook() {
		if (shutdownHook != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
			} catch (IllegalStateException ignored) {
			}
			shutdownHook = null;
		}
	}

	private void cleanupResources() {
		Printer.instance.print("正在清理资源...");
		try {
			if (context != null) {
				try {
					DataSource dataSource = context.getBean(DataSource.class);
					if (dataSource instanceof HikariDataSource) {
						((HikariDataSource) dataSource).close();
						Printer.instance.print("数据库连接池已关闭。");
					}
				} catch (Exception e) {
					Printer.instance.print("关闭数据库连接池时发生警告：" + e.getMessage());
				}
			}
			Printer.instance.print("资源清理完成。");
		} catch (Exception e) {
			Printer.instance.print("资源清理时发生错误：" + e.getMessage());
		}
	}

	public synchronized boolean isRunning() {
		return running;
	}
}
