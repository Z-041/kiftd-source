package kohgylw.kiftd.newcore.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.server.service.ResourceService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping({ "/resourceController" })
public class ResourceController {

	@Resource
	private ResourceService rs;

	@RequestMapping("/getResource/{fileId}")
	public void getResource(@PathVariable("fileId") String fileId, HttpServletRequest request,
			HttpServletResponse response) {
		rs.getResource(fileId, request, response);
	}

	@RequestMapping("/getLRContext/{fileId}")
	public void getLRContext(@PathVariable("fileId") String fileId, HttpServletRequest request,
			HttpServletResponse response) {
		rs.getLRContextByUTF8(fileId, request, response);
	}

	@RequestMapping("/getVideoTranscodeStatus.ajax")
	@ResponseBody
	public String getVideoTranscodeStatus(HttpServletRequest request) {
		return rs.getVideoTranscodeStatus(request);
	}

	@RequestMapping("/getNoticeMD5.ajax")
	@ResponseBody
	public String getNoticeMD5() {
		return rs.getNoticeMD5();
	}

	@RequestMapping("/getNoticeContext.do")
	public void getNoticeContext(HttpServletRequest request, HttpServletResponse response) {
		rs.getNoticeContext(request, response);
	}
}
