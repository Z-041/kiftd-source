package kohgylw.kiftd.server.filter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;

/**
 *
 * <h2>CSRF 防护过滤器</h2>
 * <p>
 * 采用 Double Submit Cookie 模式：为每个响应设置一个名为 XSRF-TOKEN 的 Cookie，
 * 对于会改变服务器状态的非安全请求（POST/PUT/DELETE/PATCH），要求请求头
 * X-XSRF-TOKEN 与该 Cookie 值一致。前端 AJAX 在发送请求前应读取 XSRF-TOKEN
 * Cookie 并将其放入 X-XSRF-TOKEN 请求头中。
 * </p>
 */
@WebFilter
@Order(1)
public class CsrfFilter implements Filter {

	private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
	private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
	private static final int CSRF_TOKEN_LENGTH = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;

		String existingToken = getCsrfTokenFromCookie(req);
		if (existingToken == null || existingToken.isEmpty()) {
			existingToken = generateToken();
			addCsrfCookie(resp, existingToken, req.isSecure());
		}

		if (requiresCsrfCheck(req)) {
			String requestToken = req.getHeader(CSRF_HEADER_NAME);
			if (requestToken == null || requestToken.isEmpty()) {
				// 兼容 HTML 表单提交（如打包下载 downloadCheckedFilesZip.do）：
				// 允许以同名字段携带 Token，避免原生表单无法设置自定义请求头
				requestToken = req.getParameter(CSRF_HEADER_NAME);
			}
			if (requestToken == null || !requestToken.equals(existingToken)) {
				// 直接写入 403 响应，避免 sendError 触发错误转发（ERROR dispatch）后
				// 再次进入过滤器链导致请求被资源处理器接管而变成 500
				resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
				resp.setContentType("application/json;charset=UTF-8");
				resp.getWriter().write("{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"CSRF token missing or invalid\"}");
				resp.getWriter().flush();
				return;
			}
		}

		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}

	private boolean requiresCsrfCheck(HttpServletRequest req) {
		String method = req.getMethod();
		if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
				|| "OPTIONS".equalsIgnoreCase(method) || "TRACE".equalsIgnoreCase(method)) {
			return false;
		}
		String path = req.getServletPath();
		// 仅放行登录前必须调用的只读端点（公钥获取、注册开关查询）：
		// 登录/注册页面加载时已由本过滤器下发 XSRF-TOKEN Cookie，前端钩子会将其作为
		// X-XSRF-TOKEN 请求头随登录/注册请求一并提交，因此 doLogin/doSigUp 无需豁免，
		// 避免攻击者借受害者 Cookie 跨站提交登录/注册表单（登录 CSRF）。
		if (path != null && (path.equals("/homeController/getPublicKey.ajax")
				|| path.equals("/homeController/askForAllowSignUpOrNot.ajax"))) {
			return false;
		}
		return true;
	}

	private String getCsrfTokenFromCookie(HttpServletRequest req) {
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	private void addCsrfCookie(HttpServletResponse resp, String token, boolean secure) {
		Cookie cookie = new Cookie(CSRF_COOKIE_NAME, token);
		cookie.setPath("/");
		cookie.setHttpOnly(false); // 前端需要读取
		// HTTPS 部署时强制 Secure；SameSite=Lax 缓解跨站请求伪造
		cookie.setSecure(secure);
		cookie.setAttribute("SameSite", "Lax");
		cookie.setMaxAge(-1);      // 会话 Cookie
		resp.addCookie(cookie);
	}

	private String generateToken() {
		byte[] bytes = new byte[CSRF_TOKEN_LENGTH];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
