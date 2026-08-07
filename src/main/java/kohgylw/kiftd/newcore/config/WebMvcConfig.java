package kohgylw.kiftd.newcore.config;

import java.io.File;
import java.util.Arrays;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import kohgylw.kiftd.server.util.ConfigurationManager;


@Configuration
@ComponentScan({ "kohgylw.kiftd.newcore.controller", "kohgylw.kiftd.newcore.service.impl", "kohgylw.kiftd.newcore.repository.impl", "kohgylw.kiftd.newcore.infrastructure", "kohgylw.kiftd.server.service.impl", "kohgylw.kiftd.server.util" })
@ServletComponentScan({ "kohgylw.kiftd.server.listener", "kohgylw.kiftd.server.filter", "kohgylw.kiftd.newcore.infrastructure" })
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		String origins = ConfigurationManager.instance().getCorsAllowedOrigins();
		if (origins != null && !origins.isEmpty()) {
			String[] originArray = Arrays.stream(origins.split(","))
					.map(String::trim)
					.toArray(String[]::new);
			registry.addMapping("/**")
					.allowedOrigins(originArray)
					.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
					.allowedHeaders("*");
		}
	}

	@Bean
	WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> enableDefaultServlet() {
		return (factory) -> factory.setRegisterDefaultServlet(true);
	}

	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}

	@Override
	public void addResourceHandlers(final ResourceHandlerRegistry registry) {
		// Web 端仅保留用户文件系统（webContext 目录），系统级管理统一收敛至桌面端 GUI
		final String webContextPath = "file:///" + ConfigurationManager.instance().getBasePath().replace(File.separatorChar, '/')
				+ "/webContext/";
		registry.addResourceHandler("/**").addResourceLocations(webContextPath);
	}

	/**
	 * 上传大小兜底：业务层的账户级 maxSize 由 conf/account.properties 控制，当配置缺失时
	 * 业务层不限制上传大小，存在打满磁盘的 DoS 风险。此处设置 Web 容器级绝对上限（单文件
	 * 50GB、整请求 52GB）作为兜底，仅拦截超大型上传，不影响正常业务配置。
	 */
	@Bean
	public MultipartConfigElement multipartConfigElement(ConfigurationManager cm) {
		final long maxFileSize = 50L * 1024 * 1024 * 1024; // 50GB
		final long maxRequestSize = 52L * 1024 * 1024 * 1024; // 52GB（含 multipart 开销）
		return new MultipartConfigElement(cm.getTemporaryfilePath(), maxFileSize, maxRequestSize, 0);
	}

	@Bean
	public Gson gson() {
		return new GsonBuilder().create();
	}

	@Bean
	public ServletContextInitializer sessionCookieInitializer(ConfigurationManager cm) {
		return servletContext -> {
			SessionCookieConfig config = servletContext.getSessionCookieConfig();
			config.setHttpOnly(true);
			config.setSecure(cm.isHttpsEnabled());
		};
	}

	/**
	 * 为所有响应 Cookie 追加 SameSite=Lax，缓解跨站请求伪造（CSRF）。
	 */
	@Bean
	public CookieSameSiteSupplier cookieSameSiteSupplier() {
		return CookieSameSiteSupplier.ofLax();
	}
}
