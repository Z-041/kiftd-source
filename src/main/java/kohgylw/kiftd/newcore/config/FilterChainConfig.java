package kohgylw.kiftd.newcore.config;

import java.util.List;

import jakarta.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import kohgylw.kiftd.newcore.infrastructure.logging.ApiPerformanceFilter;
import kohgylw.kiftd.newcore.infrastructure.security.ApiAuthFilter;
import kohgylw.kiftd.server.filter.CsrfFilter;
import kohgylw.kiftd.server.filter.IPFilter;
import kohgylw.kiftd.server.filter.MastLoginFilter;
import kohgylw.kiftd.server.filter.ProtectedURLFilter;
import kohgylw.kiftd.server.filter.SecurityHeadersFilter;

/**
 *
 * <h2>过滤器链显式注册配置</h2>
 * <p>
 * 原过滤器全部通过 {@code @WebFilter + @ServletComponentScan} 注册，此时 Spring 的
 * {@code @Order} 注解对 servlet 组件不保证生效，且多个过滤器同为 Order(1)（CsrfFilter
 * 与 IPFilter 等），执行顺序未定义。此处改用 FilterRegistrationBean 显式注册并设定
 * 确定顺序，保证过滤器链行为可预期：
 * </p>
 * <ol>
 * <li>ApiPerformanceFilter：最先执行，统计 /api/* 完整链路耗时；</li>
 * <li>SecurityHeadersFilter：响应安全头最先写入；</li>
 * <li>IPFilter：IP 黑白名单尽早拦截；</li>
 * <li>CsrfFilter：非安全请求 CSRF 校验；</li>
 * <li>ApiAuthFilter：/api/* 管理员认证（先于 MastLoginFilter，使 API 客户端收到 JSON 而非 HTML 重定向）；</li>
 * <li>MastLoginFilter：AJAX/页面登录校验；</li>
 * <li>ProtectedURLFilter：/prv/* 受保护页面禁止直接访问。</li>
 * </ol>
 */
@Configuration
public class FilterChainConfig {

	@Bean
	public FilterRegistrationBean<ApiPerformanceFilter> apiPerformanceFilter() {
		return register(new ApiPerformanceFilter(), "/api/*", Ordered.HIGHEST_PRECEDENCE);
	}

	@Bean
	public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
		return register(new SecurityHeadersFilter(), "/*", 0);
	}

	@Bean
	public FilterRegistrationBean<IPFilter> ipFilter() {
		return register(new IPFilter(), "/*", 1);
	}

	@Bean
	public FilterRegistrationBean<CsrfFilter> csrfFilter() {
		return register(new CsrfFilter(), "/*", 2);
	}

	@Bean
	public FilterRegistrationBean<ApiAuthFilter> apiAuthFilter() {
		return register(new ApiAuthFilter(), "/api/*", 3);
	}

	@Bean
	public FilterRegistrationBean<MastLoginFilter> mastLoginFilter() {
		return register(new MastLoginFilter(), "/*", 4);
	}

	@Bean
	public FilterRegistrationBean<ProtectedURLFilter> protectedURLFilter() {
		return register(new ProtectedURLFilter(), "/prv/*", 5);
	}

	private <T extends Filter> FilterRegistrationBean<T> register(T filter, String urlPattern, int order) {
		FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
		registration.setUrlPatterns(List.of(urlPattern));
		registration.setOrder(order);
		return registration;
	}
}
