package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface MediaService {

	String getPlayVideoJson(HttpServletRequest request);

	String getPreviewPictureJson(HttpServletRequest request);

	void getCondensedPicture(HttpServletRequest request, HttpServletResponse response);
}
