package kohgylw.kiftd.server.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * <h2>外部下载链接服务接口</h2>
 * <p>
 * 该接口定义了生成外部下载链接以及使用链接进行下载的相关操作，
 * 用于实现文件的临时分享下载功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface ExternalDownloadService {

	/**
	 *
	 * <h2>获取下载凭证</h2>
	 * <p>
	 * 针对指定资源生成一个下载凭证（key），要求请求者必须具备下载权限。
	 * 该凭证在服务器关闭前将一直有效。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含目标文件 ID
	 * @return String 下载凭证字符串
	 */
	String getDownloadKey(HttpServletRequest request);

	/**
	 *
	 * <h2>使用凭证下载文件</h2>
	 * <p>外部用户凭之前获取的下载凭证，下载对应的文件资源。</p>
	 *
	 * @param request  HttpServletRequest 请求对象，包含凭证信息
	 * @param response HttpServletResponse 响应对象，用于输出文件流
	 */
	void downloadFileByKey(HttpServletRequest request, HttpServletResponse response);

}