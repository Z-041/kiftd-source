package kohgylw.kiftd.server.filter;

import jakarta.servlet.annotation.*;
import jakarta.servlet.*;
import kohgylw.kiftd.server.util.*;
import jakarta.servlet.http.*;

import org.springframework.core.annotation.Order;

import java.io.*;

/**
 *
 * <h2>登录验证过滤器</h2>
 * <p>
 * 该过滤器用于拦截所有HTTP请求，根据系统配置的登录要求（mustLogin）检查用户是否已登录。
 * 对于需要登录但未登录的请求，根据请求类型进行不同处理：
 * HTML页面请求将被重定向至登录页，AJAX请求将返回"mustLogin"标识，
 * 而外部链接控制器、验证码获取等特定路径则直接放行。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebFilter
@Order(2)
public class MastLoginFilter implements Filter {

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final ConfigureReader cr = ConfigureReader.instance();
		final boolean s = cr.mustLogin();
		final HttpServletRequest hsq = (HttpServletRequest) request;
		final HttpServletResponse hsr = (HttpServletResponse) response;
		final String url = hsq.getServletPath().replaceAll("/{2,}", "/");
		final HttpSession session = hsq.getSession();
		if (url.startsWith("/externalLinksController")
				|| url.startsWith("/homeController/getNewVerCode.do")) {
			chain.doFilter(request, response);// 对于外部链接控制器、验证码的请求直接放行。
			return;
		}
		// 如果是无需登录的请求，那么直接放行（如果访问者已经登录，那么会被后面的过滤器重定向至主页，此处无需处理）
		switch (url) {
		case "/prv/login.html":
		case "/homeController/askForAllowSignUpOrNot.ajax":
		case "/prv/signup.html":
			chain.doFilter(request, response);
			return;
		default:
			break;
		}
		if (s) {
			if (url.equals("/") || url.endsWith(".html") || url.endsWith(".do")) {
				if (session.getAttribute("ACCOUNT") != null) {
					final String account = (String) session.getAttribute("ACCOUNT");
					if (cr.foundAccount(account)) {
						chain.doFilter(request, response);
					} else {
						hsr.sendRedirect("/prv/login.html");
					}
				} else {
					hsr.sendRedirect("/prv/login.html");
				}
			} else if (url.endsWith(".ajax")) {
				if (url.equals("/homeController/doLogin.ajax") || url.equals("/homeController/getPublicKey.ajax")
						|| url.equals("/homeController/doSigUp.ajax")) {
					chain.doFilter(request, response);
				} else if (session.getAttribute("ACCOUNT") != null) {
					final String account = (String) session.getAttribute("ACCOUNT");
					if (cr.foundAccount(account)) {
						chain.doFilter(request, response);
					} else {
						hsr.setCharacterEncoding("UTF-8");
						final PrintWriter pw = hsr.getWriter();
						pw.print("mustLogin");
						pw.flush();
					}
				} else {
					hsr.setCharacterEncoding("UTF-8");
					final PrintWriter pw2 = hsr.getWriter();
					pw2.print("mustLogin");
					pw2.flush();
				}
			} else {
				chain.doFilter(request, response);
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}

}
