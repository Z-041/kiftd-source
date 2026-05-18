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

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.IpAddrGetter;

/**
 * 
 * <h2>阻止特定IP访问过滤器</h2>
 * <p>
 * 该过滤器用于根据系统配置的IP访问规则，阻止特定IP地址的HTTP请求，
 * 从而保护服务器资源免受未授权的访问。如果请求的IP不在允许范围内，
 * 则返回 HTTP 403 Forbidden 状态码。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebFilter
@Order(1)
public class IPFilter implements Filter {

	private IpAddrGetter idg;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		ApplicationContext context = WebApplicationContextUtils
				.getWebApplicationContext(filterConfig.getServletContext());
		idg = context.getBean(IpAddrGetter.class);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (ConfigureReader.instance().enableIPRule()) {
			HttpServletRequest hsr = (HttpServletRequest) request;
			if (ConfigureReader.instance().filterAccessIP(idg.getIpAddr(hsr))) {
				chain.doFilter(request, response);
			} else {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}

}
