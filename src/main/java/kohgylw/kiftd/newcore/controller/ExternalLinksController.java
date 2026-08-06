package kohgylw.kiftd.newcore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import kohgylw.kiftd.newcore.service.ExternalDownloadService;
import kohgylw.kiftd.newcore.service.FileChainService;


@Controller
@RequestMapping({ "/externalLinksController" })
public class ExternalLinksController {

	private final ExternalDownloadService eds;
	private final FileChainService fcs;

	public ExternalLinksController(ExternalDownloadService eds, FileChainService fcs) {
		this.eds = eds;
		this.fcs = fcs;
	}

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
