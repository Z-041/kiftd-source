package kohgylw.kiftd.newcore.infrastructure.logging.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import kohgylw.kiftd.newcore.infrastructure.logging.LogService;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.LogUtil;

import java.util.List;

@Service
@Primary
public class LogServiceImpl implements LogService {

	private final LogUtil logUtil;

	public LogServiceImpl(LogUtil logUtil) {
		this.logUtil = logUtil;
	}

	@Override
	public void writeException(Exception e) {
		logUtil.writeException(e);
	}

	@Override
	public void writeCreateFolderEvent(String account, String ip, Folder folder) {
		logUtil.writeCreateFolderEvent(account, ip, folder);
	}

	@Override
	public void writeDeleteFolderEvent(HttpServletRequest request, Folder folder, List<Folder> parentList) {
		logUtil.writeDeleteFolderEvent(request, folder, parentList);
	}

	@Override
	public void writeRenameFolderEvent(String account, String ip, String folderId, String oldName, String newName,
			String oldConstraint, String newConstraint) {
		logUtil.writeRenameFolderEvent(account, ip, folderId, oldName, newName, oldConstraint, newConstraint);
	}

	@Override
	public void writeUploadFileEvent(HttpServletRequest request, Node file, String account) {
		logUtil.writeUploadFileEvent(request, file, account);
	}

	@Override
	public void writeDeleteFileEvent(HttpServletRequest request, Node file) {
		logUtil.writeDeleteFileEvent(request, file);
	}

	@Override
	public void writeDownloadFileEvent(String account, String ip, Node file) {
		logUtil.writeDownloadFileEvent(account, ip, file);
	}

	@Override
	public void writeRenameFileEvent(String account, String ip, String parentFolderId, String oldName, String newName) {
		logUtil.writeRenameFileEvent(account, ip, parentFolderId, oldName, newName);
	}

	@Override
	public void writeMoveFileEvent(String account, String ip, String originPath, String finalPath, boolean isCopy) {
		logUtil.writeMoveFileEvent(account, ip, originPath, finalPath, isCopy);
	}

	@Override
	public void writeMoveFolderEvent(String account, String ip, String originPath, String finalPath, boolean isCopy) {
		logUtil.writeMoveFolderEvent(account, ip, originPath, finalPath, isCopy);
	}

	@Override
	public void writeChainEvent(HttpServletRequest request, Node file) {
		logUtil.writeChainEvent(request, file);
	}

	@Override
	public void writeDownloadFileByKeyEvent(HttpServletRequest request, Node file) {
		logUtil.writeDownloadFileByKeyEvent(request, file);
	}

	@Override
	public void writeShareFileURLEvent(HttpServletRequest request, Node file) {
		logUtil.writeShareFileURLEvent(request, file);
	}

	@Override
	public void writeDownloadCheckedFileEvent(HttpServletRequest request, List<String> idList, List<String> fidList) {
		logUtil.writeDownloadCheckedFileEvent(request, idList, fidList);
	}

	@Override
	public void writeChangePasswordEvent(HttpServletRequest request, String account) {
		logUtil.writeChangePasswordEvent(request, account);
	}

	@Override
	public void writeSignUpEvent(HttpServletRequest request, String account) {
		logUtil.writeSignUpEvent(request, account);
	}
}
