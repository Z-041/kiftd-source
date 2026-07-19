package kohgylw.kiftd.newcore.config;

import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

@Configuration
public class UndertowServerConfig {

	@Bean
	public UndertowServletWebServerFactory servletContainer(ConfigurationManager cm) {
		UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();

		configureHttps(factory, cm);
		configureErrorPages(factory);
		configureThreadPools(factory);

		return factory;
	}

	private void configureHttps(UndertowServletWebServerFactory factory, ConfigurationManager cm) {
		if (cm.isHttpsEnabled()) {
			int httpPort = cm.getPort();
			int httpsPort = cm.getHttpsPort();

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
	}

	private void configureErrorPages(UndertowServletWebServerFactory factory) {
		factory.addErrorPages(
				new ErrorPage(HttpStatus.NOT_FOUND, "/prv/error.html"),
				new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/prv/error.html"),
				new ErrorPage(HttpStatus.UNAUTHORIZED, "/prv/error.html"),
				new ErrorPage(HttpStatus.FORBIDDEN, "/prv/forbidden.html"));
	}

	private void configureThreadPools(UndertowServletWebServerFactory factory) {
		factory.addBuilderCustomizers(builder -> {
			builder.setWorkerThreads(200);
			builder.setIoThreads(Math.max(Runtime.getRuntime().availableProcessors(), 2));
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
			builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 30000);
		});
	}

	private HttpHandler httpToHttpsRedirect(HttpHandler next, ConfigurationManager cm) {
		return exchange -> {
			if ("http".equals(exchange.getRequestScheme())) {
				int httpsPort = cm.getHttpsPort();
				String host = cm.getHttpsRedirectHost();
				if (host == null || host.isEmpty()) {
					host = exchange.getDestinationAddress().getHostString();
				}
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
