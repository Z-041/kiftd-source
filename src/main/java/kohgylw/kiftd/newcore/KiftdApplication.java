package kohgylw.kiftd.newcore;

import kohgylw.kiftd.newcore.config.WebMvcConfig;
import kohgylw.kiftd.newcore.config.DataSourceConfig;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.newcore.config.ConfigurationManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

@SpringBootApplication
@Import({ WebMvcConfig.class, DataSourceConfig.class, ConfigurationManager.class })
public class KiftdApplication {

	private static ApplicationContext context;
	private static boolean running;

	static {
		System.setProperty("logging.level.root", "ERROR");
		running = false;
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
					Printer.instance.print("服务器引擎已启动。");
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

	public synchronized boolean stop() {
		Printer.instance.print("正在关闭服务器...");
		if (context != null) {
			Printer.instance.print("正在终止服务器引擎...");
			try {
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

	public synchronized boolean isRunning() {
		return running;
	}

	@Bean
	public ServletWebServerFactory servletContainer(ConfigurationManager cm) {
		UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();

		if (cm.isHttpsEnabled()) {
			final int httpPort = cm.getPort();
			final int httpsPort = cm.getHttpsPort();

			factory.setPort(httpsPort);

			Ssl ssl = new Ssl();
			ssl.setKeyStore(cm.getHttpsKeyFile());
			ssl.setKeyStorePassword(cm.getHttpsKeyPass());
			ssl.setKeyStoreType(cm.getHttpsKeyType());
			factory.setSsl(ssl);

			factory.addBuilderCustomizers(builder -> {
				builder.addHttpListener(httpPort, "0.0.0.0");
				builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true);
			});

			factory.addDeploymentInfoCustomizers(deploymentInfo -> {
				deploymentInfo.addInitialHandlerChainWrapper(handler -> httpToHttpsRedirect(handler, cm));
			});
		} else {
			factory.setPort(cm.getPort());
		}

		factory.addErrorPages(
				new ErrorPage(HttpStatus.NOT_FOUND, "/prv/error.html"),
				new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/prv/error.html"),
				new ErrorPage(HttpStatus.UNAUTHORIZED, "/prv/error.html"),
				new ErrorPage(HttpStatus.FORBIDDEN, "/prv/forbidden.html"));

		factory.addBuilderCustomizers(builder -> {
			builder.setWorkerThreads(200);
			builder.setIoThreads(Math.max(Runtime.getRuntime().availableProcessors(), 2));
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
			builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 30000);
		});

		return factory;
	}

	private HttpHandler httpToHttpsRedirect(HttpHandler next, ConfigurationManager cm) {
		return exchange -> {
			if ("http".equals(exchange.getRequestScheme())) {
				int httpsPort = cm.getHttpsPort();
				String host = exchange.getHostName();
				String path = exchange.getRequestURI();
				String query = exchange.getQueryString();
				String location = "https://" + host + ":" + httpsPort + path;
				if (query != null && !query.isEmpty()) {
					location += "?" + query;
				}
				exchange.setStatusCode(StatusCodes.FOUND);
				exchange.getResponseHeaders().put(Headers.LOCATION, location);
				return;
			}
			next.handleRequest(exchange);
		};
	}
}
