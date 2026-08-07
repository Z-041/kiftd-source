package kohgylw.kiftd.newcore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.newcore.service.FolderViewService;


@Controller
@RequestMapping({ "/homeController" })
public class FolderController {

	private static final String CHARSET_BY_AJAX = "text/html; charset=utf-8";

	private final FolderViewService folderViewService;
	private final FolderService folderService;

	public FolderController(FolderViewService folderViewService, FolderService folderService) {
		this.folderViewService = folderViewService;
		this.folderService = folderService;
	}

	@RequestMapping(value = { "/getFolderView.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getFolderView(final String fid, final HttpSession session, final HttpServletRequest request) {
		return folderViewService.getFolderViewJson(fid, session, request);
	}

	@RequestMapping(value = { "/getRemainingFolderView.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getRemainingFolderView(final HttpServletRequest request) {
		return folderViewService.getRemainingFolderViewJson(request);
	}

	@PostMapping({ "/newFolder.ajax" })
	@ResponseBody
	public String newFolder(final HttpServletRequest request) {
		return this.folderService.newFolder(request);
	}

	@PostMapping({ "/deleteFolder.ajax" })
	@ResponseBody
	public String deleteFolder(final HttpServletRequest request) {
		return this.folderService.deleteFolder(request);
	}

	@PostMapping({ "/renameFolder.ajax" })
	@ResponseBody
	public String renameFolder(final HttpServletRequest request) {
		return this.folderService.renameFolder(request);
	}

	@RequestMapping(value = { "/countFolderContent.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String countFolderContent(final HttpServletRequest request) {
		return folderService.getFolderCountResult(request);
	}
}
