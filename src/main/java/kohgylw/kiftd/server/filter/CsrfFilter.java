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
			addCsrfCookie(resp, existingToken);
		}

		if (requiresCsrfCheck(req)) {
			String requestToken = req.getHeader(CSRF_HEADER_NAME);
			if (requestToken == null || !requestToken.equals(existingToken)) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token missing or invalid");
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
		// 登录、注册、公钥等公开接口在登录前无法携带 Cookie，予以放行
		if (path != null && (path.equals("/homeController/doLogin.ajax")
				|| path.equals("/homeController/doSigUp.ajax")
				|| path.equals("/homeController/getPublicKey.ajax")
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

	private void addCsrfCookie(HttpServletResponse resp, String token) {
		Cookie cookie = new Cookie(CSRF_COOKIE_NAME, token);
		cookie.setPath("/");
		cookie.setHttpOnly(false); // 前端需要读取
		cookie.setSecure(false);   // 保持与当前请求通道一致即可，部署 HTTPS 时建议改为 true
		cookie.setMaxAge(-1);      // 会话 Cookie
		resp.addCookie(cookie);
	}

	private String generateToken() {
		byte[] bytes = new byte[CSRF_TOKEN_LENGTH];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
