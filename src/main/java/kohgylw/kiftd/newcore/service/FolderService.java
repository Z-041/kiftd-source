package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;

public interface FolderService {

	String newFolder(HttpServletRequest request);

	String deleteFolder(HttpServletRequest request);

	String renameFolder(HttpServletRequest request);

	String deleteFolderByName(HttpServletRequest request);

	String createNewFolderByName(HttpServletRequest request);

	String getFolderCountResult(HttpServletRequest request);
}
