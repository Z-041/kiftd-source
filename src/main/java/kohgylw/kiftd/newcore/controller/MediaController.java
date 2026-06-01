package kohgylw.kiftd.newcore.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.newcore.service.MediaService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping({ "/homeController" })
public class MediaController {

	private static final String CHARSET_BY_AJAX = "text/html; charset=utf-8";

	@Resource
	private MediaService mediaService;

	@RequestMapping(value = { "/playVideo.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String playVideo(final HttpServletRequest request, final HttpServletResponse response) {
		return this.mediaService.getPlayVideoJson(request);
	}

	@RequestMapping(value = { "/getPrePicture.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getPrePicture(final HttpServletRequest request) {
		return this.mediaService.getPreviewPictureJson(request);
	}

	@RequestMapping({ "/showCondensedPicture.do" })
	public void showCondensedPicture(final HttpServletRequest request, final HttpServletResponse response) {
		mediaService.getCondensedPicture(request, response);
	}
}
