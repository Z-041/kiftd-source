package kohgylw.kiftd.server.bootstrap;

import org.springframework.boot.web.servlet.server.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import kohgylw.kiftd.server.configation.*;
import org.springframework.context.*;
import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.util.*;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.boot.*;
import org.springframework.http.*;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.*;

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

	/**
	 * 
	 * <h2>启动服务器</h2>
	 * <p>
	 * 该方法将启动SpringBoot服务器引擎并返回启动结果。该过程较为耗时，为了不阻塞主线程，请在额外线程中执行该方法。
	 * </p>
	 * 
	 * @return boolean 启动结果
	 */
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

	/**
	 * 
	 * <h2>停止服务器</h2>
	 * <p>
	 * 该方法将关闭服务器引擎并清理缓存文件。该方法较为耗时。
	 * </p>
	 * 
	 * @return boolean 关闭结果
	 */
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

	/**
	 * 
	 * <h2>获取服务器运行状态</h2>
	 * <p>
	 * 该方法返回服务器引擎的运行状态，该状态由内置属性记录，且唯一。
	 * </p>
	 * 
	 * @return boolean 服务器是否启动
	 */
	public synchronized boolean started() {
		return KiftdCtl.run;
	}

	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = null;
		if (ConfigureReader.instance().openHttps()) {
			tomcat = new TomcatServletWebServerFactory() {
				@Override
				protected void customizeConnector(Connector connector) {
					connector.setScheme("http");
					connector.setPort(ConfigureReader.instance().getPort());
					connector.setSecure(false);
					connector.setRedirectPort(ConfigureReader.instance().getHttpsPort());
				}

				@Override
				protected void postProcessContext(Context context) {
					SecurityConstraint constraint = new SecurityConstraint();
					constraint.setUserConstraint("CONFIDENTIAL");
					SecurityCollection collection = new SecurityCollection();
					collection.addPattern("/*");
					constraint.addCollection(collection);
					context.addConstraint(constraint);
				}
			};
			tomcat.addAdditionalTomcatConnectors(createHttpsConnector());
		} else {
			tomcat = new TomcatServletWebServerFactory();
			tomcat.setPort(ConfigureReader.instance().getPort());
		}
		tomcat.addErrorPages(new ErrorPage[] { new ErrorPage(HttpStatus.NOT_FOUND, "/prv/error.html"),
				new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/prv/error.html"),
				new ErrorPage(HttpStatus.UNAUTHORIZED, "/prv/error.html"),
				new ErrorPage(HttpStatus.FORBIDDEN, "/prv/forbidden.html") });
		tomcat.addConnectorCustomizers(connector -> {
			Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
			protocol.setMaxThreads(200);
			protocol.setMaxConnections(10000);
			protocol.setConnectionTimeout(30000);
			protocol.setMinSpareThreads(10);
			connector.setProperty("compression", "on");
			connector.setProperty("compressibleMimeType",
					"text/html,text/xml,text/plain,text/css,text/javascript,application/json,application/javascript");
		});
		return tomcat;
	}

	private Connector createHttpsConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setScheme("https");
		connector.setPort(ConfigureReader.instance().getHttpsPort());
		connector.setSecure(true);
		Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
		protocol.setSSLEnabled(true);
		SSLHostConfig sslHostConfig = new SSLHostConfig();
		SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
		certificate.setCertificateKeystoreFile(ConfigureReader.instance().getHttpsKeyFile());
		certificate.setCertificateKeystoreType(ConfigureReader.instance().getHttpsKeyType());
		certificate.setCertificateKeystorePassword(ConfigureReader.instance().getHttpsKeyPass());
		sslHostConfig.addCertificate(certificate);
		protocol.addSslHostConfig(sslHostConfig);
		return connector;
	}
}