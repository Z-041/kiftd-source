package kohgylw.kiftd.newcore.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.service.FileService;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.newcore.service.FolderViewService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping({ "/homeController" })
public class FileController {

	private static final String CHARSET_BY_AJAX = "text/html; charset=utf-8";

	@Resource
	private FileService fileService;
	@Resource
	private FolderService folderService;
	@Resource
	private FolderViewService folderViewService;

	@RequestMapping(value = { "/douploadFile.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String douploadFile(final HttpServletRequest request, final HttpServletResponse response,
			final MultipartFile file, final HttpSession session) {
		return this.fileService.doUploadFile(request, response, file);
	}

	@RequestMapping(value = { "/checkUploadFile.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session) {
		return this.fileService.checkUploadFile(request, response);
	}

	@RequestMapping(value = { "/checkImportFolder.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String checkImportFolder(final HttpServletRequest request, final HttpSession session) {
		return this.fileService.checkImportFolder(request);
	}

	@RequestMapping(value = { "/doImportFolder.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String doImportFolder(final HttpServletRequest request, final MultipartFile file,
			final HttpSession session) {
		return fileService.doImportFolder(request, file);
	}

	@RequestMapping(value = { "/deleteFolderByName.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String deleteFolderByName(final HttpServletRequest request, final HttpSession session) {
		return folderService.deleteFolderByName(request);
	}

	@RequestMapping(value = { "/createNewFolderByName.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String createNewFolderByName(final HttpServletRequest request, final HttpSession session) {
		return folderService.createNewFolderByName(request);
	}

	@RequestMapping({ "/deleteFile.ajax" })
	@ResponseBody
	public String deleteFile(final HttpServletRequest request, final HttpSession session) {
		return this.fileService.deleteFile(request);
	}

	@RequestMapping({ "/downloadFile.do" })
	public void downloadFile(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session) {
		this.fileService.doDownloadFile(request, response);
	}

	@RequestMapping({ "/renameFile.ajax" })
	@ResponseBody
	public String renameFile(final HttpServletRequest request, final HttpSession session) {
		return this.fileService.doRenameFile(request);
	}

	@RequestMapping({ "/deleteCheckedFiles.ajax" })
	@ResponseBody
	public String deleteCheckedFiles(final HttpServletRequest request, final HttpSession session) {
		return this.fileService.deleteCheckedFiles(request);
	}

	@RequestMapping({ "/getPackTime.ajax" })
	@ResponseBody
	public String getPackTime(final HttpServletRequest request, final HttpSession session) {
		return this.fileService.getPackTime(request);
	}

	@RequestMapping({ "/downloadCheckedFiles.ajax" })
	@ResponseBody
	public String downloadCheckedFiles(final HttpServletRequest request, final HttpSession session) {
		return this.fileService.downloadCheckedFiles(request);
	}

	@RequestMapping({ "/downloadCheckedFilesZip.do" })
	public void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session) throws Exception {
		this.fileService.downloadCheckedFilesZip(request, response);
	}

	@RequestMapping(value = { "/confirmMoveFiles.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String confirmMoveFiles(final HttpServletRequest request, final HttpSession session) {
		return fileService.confirmMoveFiles(request);
	}

	@RequestMapping({ "/moveCheckedFiles.ajax" })
	@ResponseBody
	public String moveCheckedFiles(final HttpServletRequest request, final HttpSession session) {
		return fileService.doMoveFiles(request);
	}

	@RequestMapping(value = { "/sreachInCompletePath.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String sreachInCompletePath(final HttpServletRequest request, final HttpSession session) {
		return folderViewService.getSearchViewJson(request);
	}
}
