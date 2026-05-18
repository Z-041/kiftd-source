package kohgylw.kiftd.server.service;

import jakarta.servlet.http.*;

/**
 *
 * <h2>图片预览服务接口</h2>
 * <p>
 * 该接口定义了与图片预览相关的业务操作，包括获取预览图片的 JSON 数据
 * 以及获取压缩版图片流以加快预览速度等功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface ShowPictureService {

	/**
	 *
	 * <h2>获取预览图片 JSON 数据</h2>
	 * <p>根据请求中的文件 ID 返回预览图片的相关信息，封装为 JSON 格式供前端 Viewer.js 插件使用。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含目标文件 ID
	 * @return String 预览图片信息的 JSON 字符串
	 */
	String getPreviewPictureJson(final HttpServletRequest request);

	/**
	 *
	 * <h2>获取压缩版图片</h2>
	 * <p>
	 * 获取指定图片的压缩版本数据流，用于加快大图片的预览速度。
	 * 压缩率会根据图片原始尺寸自动调整。
	 * </p>
	 *
	 * @param request  HttpServletRequest 请求对象，应包含文件块路径信息
	 * @param response HttpServletResponse 响应对象，用于输出压缩后的图片流
	 */
	void getCondensedPicture(final HttpServletRequest request, final HttpServletResponse response);
}