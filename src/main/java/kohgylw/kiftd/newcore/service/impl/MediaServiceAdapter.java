package kohgylw.kiftd.newcore.service.impl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.newcore.service.MediaService;
import org.springframework.stereotype.Service;

@Service
public class MediaServiceAdapter implements MediaService {

	@Resource
	private kohgylw.kiftd.server.service.PlayVideoService legacyPlayVideoService;
	@Resource
	private kohgylw.kiftd.server.service.ShowPictureService legacyShowPictureService;

	@Override
	public String getPlayVideoJson(HttpServletRequest request) {
		return legacyPlayVideoService.getPlayVideoJson(request);
	}

	@Override
	public String getPreviewPictureJson(HttpServletRequest request) {
		return legacyShowPictureService.getPreviewPictureJson(request);
	}

	@Override
	public void getCondensedPicture(HttpServletRequest request, HttpServletResponse response) {
		legacyShowPictureService.getCondensedPicture(request, response);
	}
}
