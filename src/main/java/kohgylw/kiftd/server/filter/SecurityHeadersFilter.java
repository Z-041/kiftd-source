package kohgylw.kiftd.server.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;

/**
 * <h2>安全响应头过滤器</h2>
 * <p>
 * 为所有 HTTP 响应补充基础安全响应头，缓解点击劫持、MIME 嗅探与信息泄露：
 * X-Frame-Options、X-Content-Type-Options、Referrer-Policy。
 * </p>
 */
@WebFilter
@Order(0)
public class SecurityHeadersFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponse resp = (HttpServletResponse) response;
		if (!resp.containsHeader("X-Frame-Options")) {
			resp.setHeader("X-Frame-Options", "SAMEORIGIN");
		}
		if (!resp.containsHeader("X-Content-Type-Options")) {
			resp.setHeader("X-Content-Type-Options", "nosniff");
		}
		if (!resp.containsHeader("Referrer-Policy")) {
			resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
