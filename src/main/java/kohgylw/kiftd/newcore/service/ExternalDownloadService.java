package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ExternalDownloadService {

	String getDownloadKey(HttpServletRequest request);

	void downloadFileByKey(HttpServletRequest request, HttpServletResponse response);
}
