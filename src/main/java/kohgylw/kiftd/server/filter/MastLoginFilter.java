package kohgylw.kiftd.server.filter;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.server.util.ConfigurationManager;


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
public class MastLoginFilter implements Filter {

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final ConfigurationManager cr = ConfigurationManager.instance();
		final boolean s = cr.mustLogin();
		final HttpServletRequest hsq = (HttpServletRequest) request;
		final HttpServletResponse hsr = (HttpServletResponse) response;
		final String url = hsq.getServletPath().replaceAll("/{2,}", "/");
		// 仅在有会话时读取，避免为匿名/静态资源请求无条件新建 HttpSession（会话对象堆积的内存 DoS 面）
		final HttpSession session = hsq.getSession(false);
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
				if (session != null && session.getAttribute("ACCOUNT") != null) {
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
				} else if (session != null && session.getAttribute("ACCOUNT") != null) {
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
			} else if (isStaticResource(url)) {
				// 静态资源（css/js/图片/字体等）不含业务数据，且登录页/注册页自身的样式与脚本依赖其放行
				chain.doFilter(request, response);
			} else if (session != null && session.getAttribute("ACCOUNT") != null) {
				final String account = (String) session.getAttribute("ACCOUNT");
				if (cr.foundAccount(account)) {
					chain.doFilter(request, response);
				} else {
					hsr.sendRedirect("/prv/login.html");
				}
			} else {
				hsr.sendRedirect("/prv/login.html");
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	/**
	 * 判断是否为无需登录即可访问的静态资源扩展名（不含用户业务数据）
	 */
	private boolean isStaticResource(String url) {
		int dot = url.lastIndexOf('.');
		if (dot < 0 || dot == url.length() - 1) {
			return false;
		}
		switch (url.substring(dot + 1).toLowerCase()) {
		case "css":
		case "js":
		case "map":
		case "png":
		case "jpg":
		case "jpeg":
		case "gif":
		case "svg":
		case "webp":
		case "bmp":
		case "ico":
		case "woff":
		case "woff2":
		case "ttf":
		case "eot":
		case "otf":
			return true;
		default:
			return false;
		}
	}

	@Override
	public void destroy() {
	}

}
