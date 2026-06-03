package kohgylw.kiftd.newcore.config;

import java.io.File;

import jakarta.servlet.MultipartConfigElement;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Configuration
@ComponentScan({ "kohgylw.kiftd.newcore.controller", "kohgylw.kiftd.newcore.service.impl", "kohgylw.kiftd.newcore.repository.impl", "kohgylw.kiftd.newcore.infrastructure", "kohgylw.kiftd.server.service.impl", "kohgylw.kiftd.server.util" })
@ServletComponentScan({ "kohgylw.kiftd.server.listener", "kohgylw.kiftd.server.filter" })
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOriginPatterns("*")
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
					throws Exception {
				if ("POST".equalsIgnoreCase(request.getMethod())
						&& request.getRequestURI().endsWith(".ajax")) {
					String referer = request.getHeader("Referer");
					String origin = request.getHeader("Origin");
					if (referer == null && origin == null) {
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
						return false;
					}
				}
				return true;
			}
		});
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
		final String webContextPath = "file:///" + ConfigurationManager.instance().getBasePath().replace(File.separatorChar, '/')
				+ "/webContext/";
		registry.addResourceHandler("/**").addResourceLocations(webContextPath);
	}

	@Bean
	public MultipartConfigElement multipartConfigElement(ConfigurationManager cm) {
		return new MultipartConfigElement(cm.getTemporaryfilePath());
	}

	@Bean
	public Gson gson() {
		return new GsonBuilder().create();
	}
}
