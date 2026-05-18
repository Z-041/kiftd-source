package kohgylw.kiftd.server.service;

import jakarta.servlet.http.*;

/**
 *
 * <h2>视频播放服务接口</h2>
 * <p>
 * 该接口定义了视频播放相关的业务操作，主要根据视频文件 ID 查询文件节点、
 * 判断是否需要转码并返回节点的 JSON 信息，供前端页面发起播放请求。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface PlayVideoService {

	/**
	 *
	 * <h2>获取视频播放 JSON 信息</h2>
	 * <p>
	 * 根据视频文件的 ID 查询文件节点，判断是否需要转码，并返回包含文件名、
	 * 文件大小、文件路径等信息的 JSON 字符串，供前端播放器加载。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含视频文件 ID
	 * @return String 视频节点信息的 JSON 字符串，由 VideoInfo POJO 转化而来
	 */
	String getPlayVideoJson(final HttpServletRequest request);
}