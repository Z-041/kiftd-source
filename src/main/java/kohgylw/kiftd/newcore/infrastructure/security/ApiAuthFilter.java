package kohgylw.kiftd.newcore.infrastructure.security;

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
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;

import org.springframework.core.annotation.Order;

import kohgylw.kiftd.newcore.domain.ApiResponse;
import kohgylw.kiftd.newcore.domain.ResultCode;
import kohgylw.kiftd.server.util.ConfigurationManager;

/**
 *
 * <h2>API 通道管理员认证过滤器</h2>
 * <p>
 * 对 "/api/*" 统一实施管理员认证：未登录返回 401 JSON，已登录但非管理员返回 403 JSON，
 * 通过后放行。将认证逻辑从各 API 控制器内嵌的 requireAdmin 收敛到过滤链，与 AJAX 通道
 * 的 MastLoginFilter 职责对等，消除"API 通道认证手写在控制器内"的架构债务。
 * 置于 MastLoginFilter 之前执行，使 API 客户端未登录时收到结构化 401 JSON，
 * 而非面向浏览器的 HTML 重定向。
 * </p>
 *
 * @author 技术债治理迭代
 * @version 1.0
 */
@WebFilter(urlPatterns = "/api/*")
@Order(1)
public class ApiAuthFilter implements Filter {

	private static final String SESSION_ACCOUNT_ATTR = "ACCOUNT";
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";

	private final Gson gson = new Gson();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;

		HttpSession session = req.getSession(false);
		String account = session == null ? null : (String) session.getAttribute(SESSION_ACCOUNT_ATTR);

		if (account == null) {
			writeJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.UNAUTHORIZED);
			return;
		}
		if (!ConfigurationManager.instance().isSuperAdmin(account)) {
			writeJsonError(resp, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN);
			return;
		}
		chain.doFilter(request, response);
	}

	private void writeJsonError(HttpServletResponse resp, int statusCode, ResultCode resultCode) throws IOException {
		if (resp.isCommitted()) {
			return;
		}
		resp.setStatus(statusCode);
		resp.setContentType(JSON_UTF8);
		resp.getWriter().write(gson.toJson(ApiResponse.failure(resultCode.getCode(), resultCode.getMessage())));
		resp.getWriter().flush();
	}

	@Override
	public void destroy() {
	}
}
