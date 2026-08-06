package kohgylw.kiftd.newcore.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.google.gson.Gson;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.domain.AjaxProtocol;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.newcore.service.FolderViewService;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.FolderView;
import kohgylw.kiftd.server.pojo.RemainingFolderView;
import kohgylw.kiftd.server.pojo.SearchView;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;
import kohgylw.kiftd.server.util.ServerTimeUtil;


@Primary
@Service
public class FolderViewServiceImpl implements FolderViewService {

	private static int SELECT_STEP = 256;

	/**
	 * 搜索结果上限：防止超大目录下搜索一次性返回/加载海量结果导致前端渲染爆炸或 OOM。
	 * 文件夹与文件结果各自独立限流；文件查询在 SQL 层 LIMIT 下推（见 {@link #SEARCH_FILE_QUERY_LIMIT}）。
	 */
	private static final int SEARCH_RESULT_LIMIT = 500;

	/**
	 * 搜索文件查询的 SQL 层 LIMIT：与结果上限对齐，避免 DB 全量加载所有后代文件后再在内存过滤。
	 */
	private static final int SEARCH_FILE_QUERY_LIMIT = 500;

	private final FolderRepository folderRepository;
	private final FileNodeRepository fileNodeRepository;
	private final FolderUtil folderUtil;
	private final Gson gson;
	private final KiftdFFMPEGLocator kiftdFFMPEGLocator;

	public FolderViewServiceImpl(FolderRepository folderRepository, FileNodeRepository fileNodeRepository,
			FolderUtil folderUtil, Gson gson, KiftdFFMPEGLocator kiftdFFMPEGLocator) {
		this.folderRepository = folderRepository;
		this.fileNodeRepository = fileNodeRepository;
		this.folderUtil = folderUtil;
		this.gson = gson;
		this.kiftdFFMPEGLocator = kiftdFFMPEGLocator;
	}

	@Override
	public String getFolderViewJson(final String fid, final HttpSession session, final HttpServletRequest request) {
		final ConfigurationManager cr = ConfigurationManager.instance();
		if (fid == null || fid.length() == 0) {
			return AjaxProtocol.ERROR;
		}
		Folder vf = this.folderRepository.selectById(fid);
		if (vf == null) {
			return AjaxProtocol.NOT_FOUND;
		}
		final String account = (String) session.getAttribute("ACCOUNT");
		if (!ConfigurationManager.instance().accessFolder(vf, account)) {
			return AjaxProtocol.NOT_ACCESS;
		}
		final FolderView fv = new FolderView();
		fv.setSelectStep(SELECT_STEP);
		fv.setFolder(vf);
		fv.setParentList(this.folderUtil.getParentList(fid));
		long foldersOffset = this.folderRepository.countByParentId(fid);
		fv.setFoldersOffset(foldersOffset);
		Map<String, Object> keyMap1 = new HashMap<>();
		keyMap1.put("pid", fid);
		long fOffset = foldersOffset - SELECT_STEP;
		keyMap1.put("offset", fOffset > 0L ? fOffset : 0L);
		keyMap1.put("rows", SELECT_STEP);
		List<Folder> folders = this.folderRepository.selectByParentIdSection(keyMap1);
		List<Folder> fs = folders.stream()
				.filter(f -> cr.accessFolder(f, account))
				.collect(Collectors.toList());
		fv.setFolderList(fs);
		long filesOffset = this.fileNodeRepository.countByParentFolderId(fid);
		fv.setFilesOffset(filesOffset);
		Map<String, Object> keyMap2 = new HashMap<>();
		keyMap2.put("pfid", fid);
		long fiOffset = filesOffset - SELECT_STEP;
		keyMap2.put("offset", fiOffset > 0L ? fiOffset : 0L);
		keyMap2.put("rows", SELECT_STEP);
		fv.setFileList(this.fileNodeRepository.selectByParentFolderIdSection(keyMap2));
		if (account != null) {
			fv.setAccount(account);
		}
		fv.setAllowChangePassword(String.valueOf(cr.isAllowChangePassword()));
		fv.setAllowSignUp(String.valueOf(cr.isAllowSignUp()));
		final List<String> authList = new ArrayList<String>();
		if (cr.authorized(account, AccountAuth.UPLOAD_FILES, folderUtil.getAllFoldersId(fid))) {
			authList.add("U");
		}
		if (cr.authorized(account, AccountAuth.CREATE_NEW_FOLDER, folderUtil.getAllFoldersId(fid))) {
			authList.add("C");
		}
		if (cr.authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER, folderUtil.getAllFoldersId(fid))) {
			authList.add("D");
		}
		if (cr.authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER, folderUtil.getAllFoldersId(fid))) {
			authList.add("R");
		}
		if (cr.authorized(account, AccountAuth.DOWNLOAD_FILES, folderUtil.getAllFoldersId(fid))) {
			authList.add("L");
			fv.setShowFileChain(String.valueOf(cr.isOpenFileChain()));
		}
		if (cr.authorized(account, AccountAuth.MOVE_FILES, folderUtil.getAllFoldersId(fid))) {
			authList.add("M");
		}
		fv.setAuthList(authList);
		fv.setPublishTime(ServerTimeUtil.accurateToMinute());
		fv.setEnableFFMPEG(kiftdFFMPEGLocator.isEnableFFmpeg());
		fv.setEnableDownloadZip(ConfigurationManager.instance().isEnableDownloadByZip());
		return gson.toJson(fv);
	}

	@Override
	public String getSearchViewJson(HttpServletRequest request) {
		final ConfigurationManager cr = ConfigurationManager.instance();
		String fid = request.getParameter("fid");
		String keyWorld = request.getParameter("keyworld");
		if (fid == null || fid.length() == 0 || keyWorld == null) {
			return AjaxProtocol.ERROR;
		}
		if (keyWorld.length() == 0) {
			return getFolderViewJson(fid, request.getSession(), request);
		}
		Folder vf = this.folderRepository.selectById(fid);
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (vf == null || !ConfigurationManager.instance().accessFolder(vf, account)) {
			return AjaxProtocol.NOT_ACCESS;
		}
		final SearchView sv = new SearchView();
		Folder sf = new Folder();
		sf.setFolderId(vf.getFolderId());
		sf.setFolderName("在“" + vf.getFolderName() + "”内搜索“" + keyWorld + "”的结果...");
		sf.setFolderParent(vf.getFolderId());
		sf.setFolderCreator("--");
		sf.setFolderCreationDate("--");
		sf.setFolderConstraint(vf.getFolderConstraint());
		sv.setFolder(sf);
		List<Folder> pl = this.folderUtil.getParentList(fid);
		pl.add(vf);
		sv.setParentList(pl);
		List<Node> ns = new LinkedList<>();
		List<Folder> fs = new LinkedList<>();
		searchFilesAndFolders(fid, keyWorld.toUpperCase(), account, ns, fs);
		sv.setFileList(ns);
		sv.setFolderList(fs);
		sv.setFoldersOffset(0L);
		sv.setFilesOffset(0L);
		sv.setSelectStep(SELECT_STEP);
		if (account != null) {
			sv.setAccount(account);
		}
		sv.setAllowChangePassword(String.valueOf(cr.isAllowChangePassword()));
		final List<String> authList = new ArrayList<String>();
		if (cr.authorized(account, AccountAuth.DOWNLOAD_FILES, folderUtil.getAllFoldersId(fid))) {
			authList.add("L");
			sv.setShowFileChain(String.valueOf(cr.isOpenFileChain()));
		}
		authList.add("O");
		sv.setAuthList(authList);
		sv.setPublishTime(ServerTimeUtil.accurateToMinute());
		sv.setKeyWorld(keyWorld);
		sv.setEnableFFMPEG(kiftdFFMPEGLocator.isEnableFFmpeg());
		sv.setEnableDownloadZip(ConfigurationManager.instance().isEnableDownloadByZip());
		return gson.toJson(sv);
	}

	private void searchFilesAndFolders(String fid, String key, String account, List<Node> ns, List<Folder> fs) {
		final List<Folder> allFolders = this.folderUtil.getAllDescendantFolders(fid);
		final Set<String> accessibleIds = new HashSet<>();
		accessibleIds.add(fid);
		final ConfigurationManager cr = ConfigurationManager.instance();
		for (final Folder f : allFolders) {
			if (accessibleIds.contains(f.getFolderParent()) && cr.accessFolder(f, account)) {
				accessibleIds.add(f.getFolderId());
				if (fs.size() < SEARCH_RESULT_LIMIT && f.getFolderName().toUpperCase().indexOf(key) >= 0) {
					fs.add(f);
				}
			}
		}
		// SQL 层 LIMIT 下推：仅加载结果上限数量的文件，避免全量加载后代文件造成慢查询与 OOM
		final List<Node> allFiles = this.fileNodeRepository
				.selectByParentFolderIdsLimit(new ArrayList<>(accessibleIds), SEARCH_FILE_QUERY_LIMIT);
		allFiles.stream()
				.filter(n -> n.getFileName().toUpperCase().indexOf(key) >= 0)
				.limit(SEARCH_RESULT_LIMIT)
				.forEach(ns::add);
	}

	@Override
	public String getRemainingFolderViewJson(HttpServletRequest request) {
		final String fid = request.getParameter("fid");
		final String foldersOffset = request.getParameter("foldersOffset");
		final String filesOffset = request.getParameter("filesOffset");
		if (fid == null || fid.length() == 0) {
			return AjaxProtocol.ERROR;
		}
		Folder vf = this.folderRepository.selectById(fid);
		if (vf == null) {
			return AjaxProtocol.NOT_FOUND;
		}
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final ConfigurationManager cr = ConfigurationManager.instance();
		if (!cr.accessFolder(vf, account)) {
			return AjaxProtocol.NOT_ACCESS;
		}
		final RemainingFolderView fv = new RemainingFolderView();
		if (foldersOffset != null) {
			try {
				long newFoldersOffset = Long.parseLong(foldersOffset);
				if (newFoldersOffset > 0L) {
					Map<String, Object> keyMap1 = new HashMap<>();
					keyMap1.put("pid", fid);
					long nfOffset = newFoldersOffset - SELECT_STEP;
					keyMap1.put("offset", nfOffset > 0L ? nfOffset : 0L);
					keyMap1.put("rows", nfOffset > 0L ? SELECT_STEP : newFoldersOffset);
					List<Folder> folders = this.folderRepository.selectByParentIdSection(keyMap1);
					List<Folder> fs = folders.stream()
							.filter(f -> cr.accessFolder(f, account))
							.collect(Collectors.toList());
					fv.setFolderList(fs);
				}
			} catch (NumberFormatException e) {
				return AjaxProtocol.ERROR;
			}
		}
		if (filesOffset != null) {
			try {
				long newFilesOffset = Long.parseLong(filesOffset);
				if (newFilesOffset > 0L) {
					Map<String, Object> keyMap2 = new HashMap<>();
					keyMap2.put("pfid", fid);
					long nfiOffset = newFilesOffset - SELECT_STEP;
					keyMap2.put("offset", nfiOffset > 0L ? nfiOffset : 0L);
					keyMap2.put("rows", nfiOffset > 0L ? SELECT_STEP : newFilesOffset);
					fv.setFileList(this.fileNodeRepository.selectByParentFolderIdSection(keyMap2));
				}
			} catch (NumberFormatException e) {
				return AjaxProtocol.ERROR;
			}
		}
		return gson.toJson(fv);
	}
}
