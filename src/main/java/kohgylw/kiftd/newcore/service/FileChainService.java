package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface FileChainService {

	String getChainKeyByFid(HttpServletRequest request);

	void getResourceByChainKey(HttpServletRequest request, HttpServletResponse response);
}
