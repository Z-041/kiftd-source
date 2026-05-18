package kohgylw.kiftd.server.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kohgylw.kiftd.server.service.ResourceService;

//转为在线资源打造的控制器，针对在线播放等功能的适配
@Controller
@RequestMapping("/resourceController")
public class ResourceController {

	@Resource
	private ResourceService rs;// 资源相关的服务层，详见具体实现

	// 以严格的HTTP响应格式返回在线资源流，适用于多数浏览器
	@RequestMapping("/getResource/{fileId}")
	public void getResource(@PathVariable("fileId") String fileId, HttpServletRequest request,
			HttpServletResponse response) {
		rs.getResource(fileId, request, response);
	}

	// 得到UTF-8编码格式的LRC歌词（文本）流
	@RequestMapping("/getLRContext/{fileId}")
	public void getLRContext(@PathVariable("fileId") String fileId, HttpServletRequest request,
			HttpServletResponse response) {
		rs.getLRContextByUTF8(fileId, request, response);
	}

	// 获取视频转码状态，如指定视频未开始转码则开始，如已经开始则返回进度，如已经完成则返回FIN
	@RequestMapping("/getVideoTranscodeStatus.ajax")
	public @ResponseBody String getVideoTranscodeStatus(HttpServletRequest request) {
		return rs.getVideoTranscodeStatus(request);
	}

	// 获取公告新的的MD5，如果没有公告信息，则返回null
	@RequestMapping("/getNoticeMD5.ajax")
	public @ResponseBody String getNoticeMD5() {
		return rs.getNoticeMD5();
	}

	// 获取公告信息的HTML文本流
	@RequestMapping("/getNoticeContext.do")
	public void getNoticeContext(HttpServletRequest request, HttpServletResponse response) {
		rs.getNoticeContext(request, response);
	}

}
