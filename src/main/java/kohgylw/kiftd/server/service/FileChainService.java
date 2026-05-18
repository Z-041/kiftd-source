package kohgylw.kiftd.server.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * <h2>外部资源链服务接口</h2>
 * <p>
 * 该接口处理永久资源链接（文件链）的相关操作，包括根据文件 ID 获取资源链密钥（ckey），
 * 以及通过资源链密钥返回对应的文件数据。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface FileChainService {

	/**
	 *
	 * <h2>根据文件 ID 获取资源链密钥</h2>
	 * <p>获取指定文件的永久资源链接加密密钥（ckey），用于在资源链接中标识指向的文件。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件 ID
	 * @return String 获取的 ckey，若获取失败则返回 "ERROR"
	 */
	public String getChainKeyByFid(HttpServletRequest request);

	/**
	 *
	 * <h2>根据资源链密钥返回文件数据</h2>
	 * <p>
	 * 通过永久资源链接中的 ckey 查找对应的文件并返回其数据流，同时声明适当的 Content-Type。
	 * 若未开启永久资源链接功能则返回 403 状态。
	 * </p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出文件数据流
	 */
	public void getResourceByChainKey(HttpServletRequest request, HttpServletResponse response);

}