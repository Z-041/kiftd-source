package kohgylw.kiftd.newcore.controller;

import jakarta.servlet.http.HttpServletRequest;
import kohgylw.kiftd.newcore.service.SystemService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping({ "/homeController" })
public class AdminController {

	private static final String CHARSET_BY_AJAX = "text/html; charset=utf-8";

	private final SystemService systemService;

	public AdminController(SystemService systemService) {
		this.systemService = systemService;
	}

	@RequestMapping({ "/getServerOS.ajax" })
	@ResponseBody
	public String getServerOS() {
		return this.systemService.getOSName();
	}

	@RequestMapping(value = { "/getFileChainKey.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getFileChainKey(final HttpServletRequest request) {
		return systemService.getFileChainKey(request);
	}
}
