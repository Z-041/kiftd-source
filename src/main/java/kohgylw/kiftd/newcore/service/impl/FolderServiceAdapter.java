package kohgylw.kiftd.newcore.service.impl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import kohgylw.kiftd.newcore.service.FolderService;
import org.springframework.stereotype.Service;

@Service
public class FolderServiceAdapter implements FolderService {

	@Resource
	private kohgylw.kiftd.server.service.FolderService legacyFolderService;

	@Override
	public String newFolder(HttpServletRequest request) {
		return legacyFolderService.newFolder(request);
	}

	@Override
	public String deleteFolder(HttpServletRequest request) {
		return legacyFolderService.deleteFolder(request);
	}

	@Override
	public String renameFolder(HttpServletRequest request) {
		return legacyFolderService.renameFolder(request);
	}

	@Override
	public String deleteFolderByName(HttpServletRequest request) {
		return legacyFolderService.deleteFolderByName(request);
	}

	@Override
	public String createNewFolderByName(HttpServletRequest request) {
		return legacyFolderService.createNewFolderByName(request);
	}

	@Override
	public String getFolderCountResult(HttpServletRequest request) {
		return legacyFolderService.getFolderCountResult(request);
	}
}
