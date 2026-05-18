package kohgylw.kiftd.server.service;

/**
 *
 * <h2>服务器信息服务接口</h2>
 * <p>
 * 该接口定义了获取服务器基本信息（如操作系统名称、服务器时间）的相关操作。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface ServerInfoService {

	/**
	 *
	 * <h2>获取操作系统名称</h2>
	 * <p>返回 kiftd 服务器当前运行的操作系统名称。</p>
	 *
	 * @return String 操作系统名称，例如 "Windows 10" 或 "Linux"
	 */
	String getOSName();

	/**
	 *
	 * <h2>获取服务器时间</h2>
	 * <p>返回服务器的当前系统时间字符串。</p>
	 *
	 * @return String 服务器当前时间
	 */
	String getServerTime();
}