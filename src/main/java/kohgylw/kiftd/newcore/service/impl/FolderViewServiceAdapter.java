package kohgylw.kiftd.newcore.service.impl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.service.FolderViewService;
import org.springframework.stereotype.Service;

@Service
public class FolderViewServiceAdapter implements FolderViewService {

	@Resource
	private kohgylw.kiftd.server.service.FolderViewService legacyFolderViewService;

	@Override
	public String getFolderViewJson(String folderId, HttpSession session, HttpServletRequest request) {
		return legacyFolderViewService.getFolderViewToJson(folderId, session, request);
	}

	@Override
	public String getRemainingFolderViewJson(HttpServletRequest request) {
		return legacyFolderViewService.getRemainingFolderViewToJson(request);
	}

	@Override
	public String getSearchViewJson(HttpServletRequest request) {
		return legacyFolderViewService.getSreachViewToJson(request);
	}
}
