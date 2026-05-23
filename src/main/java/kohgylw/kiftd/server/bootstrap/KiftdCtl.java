package kohgylw.kiftd.server.bootstrap;

import org.springframework.boot.web.servlet.server.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import kohgylw.kiftd.server.configation.*;
import org.springframework.context.*;
import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.util.*;

import org.springframework.boot.*;
import org.springframework.http.*;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.*;

import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

/**
 * 
 * <h2>服务器引导类</h2>
 * <p>
 * 该类为SpringBoot框架应用入口，负责初始化SpringBoot容器。同时作为服务器引擎的控制层，
 * 负责连接服务器内核与用户操作界面，用于控制服务器行为，包括启动、关闭、重启等。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@SpringBootApplication
@Import({ MVC.class })
public class KiftdCtl {
	private static ApplicationContext context;
	private static boolean run;

	static {
		System.setProperty("logging.level.root", "ERROR");
		KiftdCtl.run = false;
	}

	public synchronized boolean start() {
		Printer.instance.print("正在初始化服务器设置...");
		if (!KiftdCtl.run) {
			ConfigureReader.instance().reTestServerPropertiesAndEffect();
			if (ConfigureReader.instance().getPropertiesStatus() == 0) {
				try {
					Printer.instance.print("正在开启服务器引擎...");
					SpringApplication springApplication = new SpringApplication(KiftdCtl.class);
					springApplication.setBannerMode(Banner.Mode.OFF);
					KiftdCtl.context = springApplication.run();
					KiftdCtl.run = (KiftdCtl.context != null);
					Printer.instance.print("服务器引擎已启动。");
					return KiftdCtl.run;
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
		if (KiftdCtl.context != null) {
			Printer.instance.print("正在终止服务器引擎...");
			try {
				int exitCode = SpringApplication.exit(KiftdCtl.context, new ExitCodeGenerator[0]);
				KiftdCtl.run = false;
				KiftdCtl.context = null;
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

	public synchronized boolean started() {
		return KiftdCtl.run;
	}

	@Bean
	public ServletWebServerFactory servletContainer() {
		UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();

		if (ConfigureReader.instance().openHttps()) {
			final int httpPort = ConfigureReader.instance().getPort();
			final int httpsPort = ConfigureReader.instance().getHttpsPort();

			factory.setPort(httpsPort);

			Ssl ssl = new Ssl();
			ssl.setKeyStore(ConfigureReader.instance().getHttpsKeyFile());
			ssl.setKeyStorePassword(ConfigureReader.instance().getHttpsKeyPass());
			ssl.setKeyStoreType(ConfigureReader.instance().getHttpsKeyType());
			factory.setSsl(ssl);

			factory.addBuilderCustomizers(builder -> {
				builder.addHttpListener(httpPort, "0.0.0.0");
				builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true);
			});

			factory.addDeploymentInfoCustomizers(deploymentInfo -> {
				deploymentInfo.addInitialHandlerChainWrapper(this::httpToHttpsRedirect);
			});
		} else {
			factory.setPort(ConfigureReader.instance().getPort());
		}

		factory.addErrorPages(new ErrorPage[] { new ErrorPage(HttpStatus.NOT_FOUND, "/prv/error.html"),
				new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/prv/error.html"),
				new ErrorPage(HttpStatus.UNAUTHORIZED, "/prv/error.html"),
				new ErrorPage(HttpStatus.FORBIDDEN, "/prv/forbidden.html") });

		factory.addBuilderCustomizers(builder -> {
			builder.setWorkerThreads(200);
			builder.setIoThreads(Math.max(Runtime.getRuntime().availableProcessors(), 2));
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
			builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 30000);
		});

		return factory;
	}

	private HttpHandler httpToHttpsRedirect(HttpHandler next) {
		return exchange -> {
			if ("http".equals(exchange.getRequestScheme())) {
				int httpsPort = ConfigureReader.instance().getHttpsPort();
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
