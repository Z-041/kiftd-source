package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ResourceService {

	void getResource(String fid, HttpServletRequest request, HttpServletResponse response);

	String getVideoTranscodeStatus(HttpServletRequest request);

	void getLRContextByUTF8(String fileId, HttpServletRequest request, HttpServletResponse response);

	String getNoticeMD5();

	void getNoticeContext(HttpServletRequest request, HttpServletResponse response);
}
