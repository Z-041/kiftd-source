package kohgylw.kiftd.newcore.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.server.service.ExternalDownloadService;
import kohgylw.kiftd.server.service.FileChainService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@CrossOrigin
@RequestMapping({ "/externalLinksController" })
public class ExternalLinksController {

	@Resource
	private ExternalDownloadService eds;
	@Resource
	private FileChainService fcs;

	@RequestMapping("/getDownloadKey.ajax")
	@ResponseBody
	public String getDownloadKey(HttpServletRequest request) {
		return eds.getDownloadKey(request);
	}

	@RequestMapping("/downloadFileByKey/{fileName}")
	public void downloadFileByKey(HttpServletRequest request, HttpServletResponse response) {
		eds.downloadFileByKey(request, response);
	}

	@RequestMapping("/chain/{fileName}")
	public void chain(HttpServletRequest request, HttpServletResponse response) {
		fcs.getResourceByChainKey(request, response);
	}
}
