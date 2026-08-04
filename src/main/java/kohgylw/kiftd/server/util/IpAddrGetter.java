package kohgylw.kiftd.server.util;


import java.net.InetAddress;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * 
 * <h2>请求IP地址解析工具</h2>
 * <p>该工具包含了public String getIpAddr(HttpServletRequest request)方法用于解析某次请求的来源IP地址。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class IpAddrGetter {
	
	// 可能的转发标识请求头名称
	private static final String[] IP_ADDR_HEADERS = { "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
			"HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR" };

	/**
	 * 
	 * <h2>获得请求来源的IP地址（公网）</h2>
	 * <p>
	 * 该方法用于从请求对象中获得此请求的来源IP地址，支持反向代理。该地址将以字符串形式返回，例如“127.0.0.1”。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 请求来源IP地址 
	 */
	public String getIpAddr(HttpServletRequest request) {
		if (ConfigurationManager.instance().isIpXFFAnalysis()) {
			for (String ipAddrHeader : IP_ADDR_HEADERS) {
				String ipAddress = request.getHeader(ipAddrHeader);
				if (ipAddress != null && ipAddress.length() > 0 && !"unknown".equalsIgnoreCase(ipAddress)) {
					// XFF 链中可能包含多个转发 IP，逐一校验并返回首个合法值，避免伪造或畸形数据被直接采用
					String[] candidates = ipAddress.split(",");
					for (String candidate : candidates) {
						String trimmed = candidate.trim();
						if (isValidIp(trimmed)) {
							return trimmed;
						}
					}
				}
			}
		}
		String remoteAddr = request.getRemoteAddr();
		if (remoteAddr != null) {
			String trimmed = remoteAddr.trim();
			if (isValidIp(trimmed)) {
				return trimmed;
			}
		}
		return "获取失败";
	}

	/**
	 * 校验字符串是否为合法 IPv4 或 IPv6 地址。先通过字符集过滤避免对主机名做 DNS 查询，
	 * 再使用 InetAddress 进行格式确认。
	 */
	private boolean isValidIp(String ip) {
		if (ip == null || ip.isEmpty()) {
			return false;
		}
		// 仅允许数字、十六进制字符、点号与冒号，排除主机名
		if (!ip.matches("^[0-9a-fA-F.:]+$")) {
			return false;
		}
		try {
			InetAddress.getByName(ip);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
