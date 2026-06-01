package kohgylw.kiftd.newcore.service.impl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.newcore.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceAdapter implements FileService {

	@Resource
	private kohgylw.kiftd.server.service.FileService legacyFileService;

	@Override
	public String checkUploadFile(HttpServletRequest request, HttpServletResponse response) {
		return legacyFileService.checkUploadFile(request, response);
	}

	@Override
	public String doUploadFile(HttpServletRequest request, HttpServletResponse response, MultipartFile file) {
		return legacyFileService.doUploadFile(request, response, file);
	}

	@Override
	public String deleteFile(HttpServletRequest request) {
		return legacyFileService.deleteFile(request);
	}

	@Override
	public void doDownloadFile(HttpServletRequest request, HttpServletResponse response) {
		legacyFileService.doDownloadFile(request, response);
	}

	@Override
	public String doRenameFile(HttpServletRequest request) {
		return legacyFileService.doRenameFile(request);
	}

	@Override
	public String deleteCheckedFiles(HttpServletRequest request) {
		return legacyFileService.deleteCheckedFiles(request);
	}

	@Override
	public String getPackTime(HttpServletRequest request) {
		return legacyFileService.getPackTime(request);
	}

	@Override
	public String downloadCheckedFiles(HttpServletRequest request) {
		return legacyFileService.downloadCheckedFiles(request);
	}

	@Override
	public void downloadCheckedFilesZip(HttpServletRequest request, HttpServletResponse response) throws Exception {
		legacyFileService.downloadCheckedFilesZip(request, response);
	}

	@Override
	public String confirmMoveFiles(HttpServletRequest request) {
		return legacyFileService.confirmMoveFiles(request);
	}

	@Override
	public String doMoveFiles(HttpServletRequest request) {
		return legacyFileService.doMoveFiles(request);
	}

	@Override
	public String checkImportFolder(HttpServletRequest request) {
		return legacyFileService.checkImportFolder(request);
	}

	@Override
	public String doImportFolder(HttpServletRequest request, MultipartFile file) {
		return legacyFileService.doImportFolder(request, file);
	}
}
