package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;

public interface SystemService {

	String getOSName();

	String getFileChainKey(HttpServletRequest request);
}
