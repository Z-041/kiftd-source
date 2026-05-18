package kohgylw.kiftd.server.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;

import kohgylw.kiftd.server.util.ConfigureReader;

/**
 * 
 * <h2>受保护URL禁止直接访问过滤器</h2>
 * <p>
 * 该过滤器用于拦截对 /prv/* 路径下资源的直接访问请求。
 * 某些受保护页面（如 forbidden.html、error.html）仅允许通过服务器内部转发访问，
 * 如果用户直接通过URL访问这些页面，将被重定向至主页（home.html）。
 * 对于登录页面（login.html），如果用户已登录则自动重定向至主页，否则放行。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebFilter({ "/prv/*" })
@Order(4)
public class ProtectedURLFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest hsq = (HttpServletRequest) request;
		final HttpServletResponse hsr = (HttpServletResponse) response;
		final String url = hsq.getServletPath();
		switch (url) {
		case "/prv/forbidden.html":
		case "/prv/error.html":
			hsr.sendRedirect("/home.html");
			break;
		case "/prv/login.html":
			final String account = (String) hsq.getSession().getAttribute("ACCOUNT");
			if (ConfigureReader.instance().foundAccount(account)) {
				hsr.sendRedirect("/home.html");
			} else {
				chain.doFilter(request, response);
			}
			break;
		default:
			chain.doFilter(request, response);
			break;
		}
	}

	@Override
	public void destroy() {

	}

}
