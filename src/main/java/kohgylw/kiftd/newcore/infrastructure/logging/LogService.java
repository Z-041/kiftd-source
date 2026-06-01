package kohgylw.kiftd.newcore.infrastructure.logging;

import jakarta.servlet.http.HttpServletRequest;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;

import java.util.List;

public interface LogService {

	void writeException(Exception e);

	void writeCreateFolderEvent(String account, String ip, Folder folder);

	void writeDeleteFolderEvent(HttpServletRequest request, Folder folder, List<Folder> parentList);

	void writeRenameFolderEvent(String account, String ip, String folderId, String oldName, String newName,
			String oldConstraint, String newConstraint);

	void writeUploadFileEvent(HttpServletRequest request, Node file, String account);

	void writeDeleteFileEvent(HttpServletRequest request, Node file);

	void writeDownloadFileEvent(String account, String ip, Node file);

	void writeRenameFileEvent(String account, String ip, String parentFolderId, String oldName, String newName);

	void writeMoveFileEvent(String account, String ip, String originPath, String finalPath, boolean isCopy);

	void writeMoveFolderEvent(String account, String ip, String originPath, String finalPath, boolean isCopy);

	void writeChainEvent(HttpServletRequest request, Node file);

	void writeDownloadFileByKeyEvent(HttpServletRequest request, Node file);

	void writeShareFileURLEvent(HttpServletRequest request, Node file);

	void writeDownloadCheckedFileEvent(HttpServletRequest request, List<String> idList, List<String> fidList);

	void writeChangePasswordEvent(HttpServletRequest request, String account);

	void writeSignUpEvent(HttpServletRequest request, String account);
}
