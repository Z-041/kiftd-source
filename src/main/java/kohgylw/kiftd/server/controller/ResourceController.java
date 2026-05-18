package kohgylw.kiftd.server.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kohgylw.kiftd.server.service.ResourceService;

/**
 *
 * <h2>
 * <h2>在线资源控制器</h2>
 * <p>
 * 该控制器专门用于处理在线资源的访问请求，包括在线媒体资源播放、歌词文本获取、
 * 视频转码状态查询以及公告信息获取等功能。与 HomeController 不同，本控制器侧重于
 * 资源内容的直接传输而非文件管理操作。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Controller
@RequestMapping("/resourceController")
public class ResourceController {

	@Resource
	private ResourceService rs;

	/**
	 *
	 * <h2>获取在线资源流</h2>
	 * <p>
	 * 以标准的 HTTP 响应格式返回在线资源（如音视频文件）的数据流，
	 * 适用于大多数现代浏览器直接播放或下载。
	 * </p>
	 *
	 * @param fileId   String 资源文件的唯一标识 ID（URL 路径变量）
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出资源数据流
	 */
	@RequestMapping("/getResource/{fileId}")
	public void getResource(@PathVariable("fileId") String fileId, HttpServletRequest request,
			HttpServletResponse response) {
		rs.getResource(fileId, request, response);
	}

	/**
	 *
	 * <h2>获取 LRC 歌词文本</h2>
	 * <p>
	 * 返回指定音频文件关联的 LRC 格式歌词文本，编码为 UTF-8，
	 * 供前端音乐播放器同步显示歌词。
	 * </p>
	 *
	 * @param fileId   String 音频文件的唯一标识 ID
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出歌词文本流
	 */
	@RequestMapping("/getLRContext/{fileId}")
	public void getLRContext(@PathVariable("fileId") String fileId, HttpServletRequest request,
			HttpServletResponse response) {
		rs.getLRContextByUTF8(fileId, request, response);
	}

	/**
	 *
	 * <h2>获取视频转码状态</h2>
	 * <p>
	 * 查询指定视频文件的转码状态。若视频尚未开始转码则立即开始；
	 * 若已在转码中则返回当前进度；若转码完成则返回 "FIN"。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含目标文件 ID
	 * @return String 转码状态描述："FIN" 表示已完成，其他值表示进度百分比或 "WAIT"
	 */
	@RequestMapping("/getVideoTranscodeStatus.ajax")
	public @ResponseBody String getVideoTranscodeStatus(HttpServletRequest request) {
		return rs.getVideoTranscodeStatus(request);
	}

	/**
	 *
	 * <h2>获取公告 MD5</h2>
	 * <p>
	 * 返回当前公告内容的 MD5 校验值，用于前端判断公告是否已更新。
	 * 若没有公告信息则返回 null。
	 * </p>
	 *
	 * @return String 公告内容的 MD5 哈希值，无公告时返回 null
	 */
	@RequestMapping("/getNoticeMD5.ajax")
	public @ResponseBody String getNoticeMD5() {
		return rs.getNoticeMD5();
	}

	/**
	 *
	 * <h2>获取公告 HTML 内容</h2>
	 * <p>
	 * 将公告的 Markdown 内容渲染为 HTML 后写入响应流，供前端展示。
	 * </p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出公告 HTML 文本流
	 */
	@RequestMapping("/getNoticeContext.do")
	public void getNoticeContext(HttpServletRequest request, HttpServletResponse response) {
		rs.getNoticeContext(request, response);
	}

}