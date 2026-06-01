package kohgylw.kiftd.newcore.service.impl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import kohgylw.kiftd.newcore.service.SystemService;
import org.springframework.stereotype.Service;

@Service
public class SystemServiceAdapter implements SystemService {

	@Resource
	private kohgylw.kiftd.server.service.ServerInfoService legacyServerInfoService;
	@Resource
	private kohgylw.kiftd.server.service.FileChainService legacyFileChainService;

	@Override
	public String getOSName() {
		return legacyServerInfoService.getOSName();
	}

	@Override
	public String getFileChainKey(HttpServletRequest request) {
		return legacyFileChainService.getChainKeyByFid(request);
	}
}
