package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

	String checkUploadFile(HttpServletRequest request, HttpServletResponse response);

	String doUploadFile(HttpServletRequest request, HttpServletResponse response, MultipartFile file);

	String deleteFile(HttpServletRequest request);

	void doDownloadFile(HttpServletRequest request, HttpServletResponse response);

	String doRenameFile(HttpServletRequest request);

	String deleteCheckedFiles(HttpServletRequest request);

	String getPackTime(HttpServletRequest request);

	String downloadCheckedFiles(HttpServletRequest request);

	void downloadCheckedFilesZip(HttpServletRequest request, HttpServletResponse response) throws Exception;

	String confirmMoveFiles(HttpServletRequest request);

	String doMoveFiles(HttpServletRequest request);

	String checkImportFolder(HttpServletRequest request);

	String doImportFolder(HttpServletRequest request, MultipartFile file);
}
