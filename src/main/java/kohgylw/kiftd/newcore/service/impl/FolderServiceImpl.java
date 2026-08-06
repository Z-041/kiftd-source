package kohgylw.kiftd.newcore.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import com.google.gson.Gson;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kohgylw.kiftd.newcore.domain.AjaxProtocol;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.listener.ServerInitListener;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.CreateNewFolderByNameResponse;
import kohgylw.kiftd.server.pojo.FolderCountResult;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.FileNodeUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.util.TextFormateUtil;


/**
 *
 * <h2>文件夹服务实现类</h2>
 * <p>
 * 该类实现了 FolderService 接口中定义的文件夹相关业务逻辑，包括新建文件夹、
 * 删除文件夹、重命名文件夹、按名称删除文件夹、按名称创建文件夹以及统计文件夹内容等功能。
 * 所有操作均会进行权限校验和访问控制检查。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.newcore.service.FolderService
 */
@Service
@Primary
public class FolderServiceImpl implements FolderService {

	private final FolderRepository folderRepository;
	private final FileNodeRepository fileNodeRepository;
	private final FolderUtil folderUtil;
	private final LogUtil logUtil;
	private final Gson gson;
	private final IpAddrGetter ipAddrGetter;

	public FolderServiceImpl(FolderRepository folderRepository, FileNodeRepository fileNodeRepository,
			FolderUtil folderUtil, LogUtil logUtil, Gson gson, IpAddrGetter ipAddrGetter) {
		this.folderRepository = folderRepository;
		this.fileNodeRepository = fileNodeRepository;
		this.folderUtil = folderUtil;
		this.logUtil = logUtil;
		this.gson = gson;
		this.ipAddrGetter = ipAddrGetter;
	}

	@Transactional
	public String newFolder(final HttpServletRequest request) {
		final String parentId = request.getParameter("parentId");
		final String folderName = request.getParameter("folderName");
		final String folderConstraint = request.getParameter("folderConstraint");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (parentId == null || folderName == null || parentId.length() <= 0 || folderName.length() <= 0) {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		if (!TextFormateUtil.instance().matcherFolderName(folderName)) {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		final Folder parentFolder = this.folderRepository.selectById(parentId);
		if (parentFolder == null || !ConfigurationManager.instance().accessFolder(parentFolder, account)) {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
				folderUtil.getAllFoldersId(parentId))) {
			return AjaxProtocol.NO_AUTHORIZED;
		}
		if (folderRepository.countByParentId(parentId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return AjaxProtocol.FOLDERS_TOTAL_OUT_OF_LIMIT;
		}
		int pc = parentFolder.getFolderConstraint();
		int constraintValue;
		if (folderConstraint != null) {
			try {
				constraintValue = Integer.parseInt(folderConstraint);
				if (constraintValue != 0 && account == null) {
					return AjaxProtocol.ERROR_PARAMETER;
				}
				if (constraintValue < pc) {
					return AjaxProtocol.ERROR_PARAMETER;
				}
			} catch (NumberFormatException e) {
				return AjaxProtocol.ERROR_PARAMETER;
			}
		} else {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		int retryCount = 0;
		while (retryCount < 3) {
			if (folderRepository.selectByParentId(parentId).stream().anyMatch((e) -> e.getFolderName().equals(folderName))) {
				return AjaxProtocol.NAME_OCCUPIED;
			}
			Folder f = new Folder();
			f.setFolderId(UUID.randomUUID().toString());
			f.setFolderName(folderName);
			f.setFolderCreationDate(ServerTimeUtil.accurateToDay());
			if (account != null) {
				f.setFolderCreator(account);
			} else {
				f.setFolderCreator("匿名用户");
			}
			f.setFolderParent(parentId);
			f.setFolderConstraint(constraintValue);
			int insertRetry = 0;
			while (insertRetry < 10) {
				try {
					final int r = this.folderRepository.insert(f);
					if (r > 0) {
						if (folderUtil.isValidFolder(f)) {
							this.logUtil.writeCreateFolderEvent(account, ipAddrGetter.getIpAddr(request), f);
							return AjaxProtocol.CREATE_FOLDER_SUCCESS;
						} else {
							return AjaxProtocol.CANNOT_CREATE_FOLDER;
						}
					}
					break;
				} catch (Exception e) {
					f.setFolderId(UUID.randomUUID().toString());
					this.logUtil.writeException(e);
					insertRetry++;
				}
				if (insertRetry >= 10) {
					break;
				}
			}
			retryCount++;
		}
		return AjaxProtocol.CANNOT_CREATE_FOLDER;
	}

	@Transactional
	public String deleteFolder(final HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (folderId == null || folderId.length() == 0 || "root".equals(folderId)) {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		final Folder folder = this.folderRepository.selectById(folderId);
		if (folder == null) {
			return AjaxProtocol.DELETE_FOLDER_SUCCESS;
		}
		if (!ConfigurationManager.instance().accessFolder(folder, account)) {
			return AjaxProtocol.NO_AUTHORIZED;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				folderUtil.getAllFoldersId(folder.getFolderParent()))) {
			return AjaxProtocol.NO_AUTHORIZED;
		}
		final List<Folder> l = this.folderUtil.getParentList(folderId);
		if (this.folderRepository.deleteById(folderId) > 0) {
			folderUtil.deleteAllChildFolder(folderId);
			this.logUtil.writeDeleteFolderEvent(request, folder, l);
			ServerInitListener.needCheck = true;
			return AjaxProtocol.DELETE_FOLDER_SUCCESS;
		}
		return AjaxProtocol.CANNOT_DELETE_FOLDER;
	}

	@Transactional
	public String renameFolder(final HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		final String newName = request.getParameter("newName");
		final String folderConstraint = request.getParameter("folderConstraint");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (folderId == null || folderId.length() == 0 || newName == null || newName.length() == 0
				|| "root".equals(folderId)) {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		if (!TextFormateUtil.instance().matcherFolderName(newName)) {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		final Folder folder = this.folderRepository.selectById(folderId);
		if (folder == null) {
			return AjaxProtocol.ERROR_PARAMETER;
		}
		if (!ConfigurationManager.instance().accessFolder(folder, account)) {
			return AjaxProtocol.NO_AUTHORIZED;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER,
				folderUtil.getAllFoldersId(folder.getFolderParent()))) {
			return AjaxProtocol.NO_AUTHORIZED;
		}
		final Folder parentFolder = this.folderRepository.selectById(folder.getFolderParent());
		if (parentFolder == null) {
			// 目标文件夹的父级为空（例如父级为根目录"null"或已被删除），视为参数错误
			return AjaxProtocol.ERROR_PARAMETER;
		}
		int pc = parentFolder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int constraintValue = Integer.parseInt(folderConstraint);
				if (constraintValue > 0 && account == null) {
					return AjaxProtocol.ERROR_PARAMETER;
				}
				if (constraintValue < pc) {
					return AjaxProtocol.ERROR_PARAMETER;
				} else {
					Folder folderToUpdate = folderRepository.selectById(folderId);
					if (folderToUpdate != null) {
						folderToUpdate.setFolderConstraint(constraintValue);
						folderRepository.update(folderToUpdate);
					}
					folderUtil.changeChildFolderConstraint(folderId, constraintValue);
					if (!folder.getFolderName().equals(newName)) {
						int retryCount = 0;
						boolean success = false;
						while (retryCount < 3) {
							if (folderRepository.selectByParentId(parentFolder.getFolderId()).stream()
								.anyMatch((e) -> e.getFolderName().equals(newName))) {
								return AjaxProtocol.NAME_OCCUPIED;
							}
							Folder renameFolder = folderRepository.selectById(folderId);
							if (renameFolder != null) {
								renameFolder.setFolderName(newName);
								if (folderRepository.update(renameFolder) > 0) {
									Folder updatedFolder = folderRepository.selectById(folderId);
									if (updatedFolder != null && newName.equals(updatedFolder.getFolderName())) {
										success = true;
										break;
									}
								}
							}
							retryCount++;
						}
						if (!success) {
							return AjaxProtocol.ERROR_PARAMETER;
						}
					}
					this.logUtil.writeRenameFolderEvent(account, ipAddrGetter.getIpAddr(request), folder.getFolderId(),
							folder.getFolderName(), newName, folder.getFolderConstraint() + "", folderConstraint);
					return AjaxProtocol.RENAME_FOLDER_SUCCESS;
				}
			} catch (NumberFormatException e) {
				return AjaxProtocol.ERROR_PARAMETER;
			}
		} else {
			return AjaxProtocol.ERROR_PARAMETER;
		}
	}

	@Override
	@Transactional
	public String deleteFolderByName(HttpServletRequest request) {
		final String parentId = request.getParameter("parentId");
		final String folderName = request.getParameter("folderName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (parentId == null || parentId.length() == 0) {
			return AjaxProtocol.DELETE_ERROR;
		}
		Folder p = folderRepository.selectById(parentId);
		if (p == null) {
			return AjaxProtocol.DELETE_ERROR;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				folderUtil.getAllFoldersId(parentId)) || !ConfigurationManager.instance().accessFolder(p, account)) {
			return AjaxProtocol.DELETE_ERROR;
		}
		final Folder[] repeatFolders = this.folderRepository.selectByParentId(parentId).stream()
				.filter((f) -> f.getFolderName().equals(folderName))
				.toArray(Folder[]::new);
		for (Folder rf : repeatFolders) {
			if (!ConfigurationManager.instance().accessFolder(rf, account)) {
				return AjaxProtocol.DELETE_ERROR;
			}
			final List<Folder> l = this.folderUtil.getParentList(rf.getFolderId());
			if (this.folderRepository.deleteById(rf.getFolderId()) > 0) {
				folderUtil.deleteAllChildFolder(rf.getFolderId());
				this.logUtil.writeDeleteFolderEvent(request, rf, l);
			} else {
				return AjaxProtocol.DELETE_ERROR;
			}
		}
		ServerInitListener.needCheck = true;
		return AjaxProtocol.DELETE_SUCCESS;
	}

	@Override
	@Transactional
	public String createNewFolderByName(HttpServletRequest request) {
		final String parentId = request.getParameter("parentId");
		final String folderName = request.getParameter("folderName");
		final String folderConstraint = request.getParameter("folderConstraint");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		CreateNewFolderByNameResponse cnfbnr = new CreateNewFolderByNameResponse();
		if (parentId == null || folderName == null || parentId.length() <= 0 || folderName.length() <= 0) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		if (folderName.equals(".") || folderName.equals("..")) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		final Folder parentFolder = this.folderRepository.selectById(parentId);
		if (parentFolder == null || !ConfigurationManager.instance().accessFolder(parentFolder, account)) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
				folderUtil.getAllFoldersId(parentId))) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		if (folderRepository.countByParentId(parentId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			cnfbnr.setResult("foldersTotalOutOfLimit");
			return gson.toJson(cnfbnr);
		}
		int pc = parentFolder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int constraintValue = Integer.parseInt(folderConstraint);
				if (constraintValue != 0 && account == null) {
					cnfbnr.setResult("error");
					return gson.toJson(cnfbnr);
				}
				if (constraintValue < pc) {
					cnfbnr.setResult("error");
					return gson.toJson(cnfbnr);
				}
			} catch (NumberFormatException e) {
				cnfbnr.setResult("error");
				return gson.toJson(cnfbnr);
			}
		} else {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		int retryCount = 0;
		while (retryCount < 3) {
			Folder f = new Folder();
			List<Folder> currentFolders = folderRepository.selectByParentId(parentId);
			if (currentFolders.stream().anyMatch((e) -> e.getFolderName().equals(folderName))) {
				f.setFolderName(FileNodeUtil.getNewFolderName(folderName, currentFolders));
			} else {
				f.setFolderName(folderName);
			}
			try {
				int constraintValue = Integer.parseInt(folderConstraint);
				f.setFolderConstraint(constraintValue);
			} catch (NumberFormatException e) {
				cnfbnr.setResult("error");
				return gson.toJson(cnfbnr);
			}
			f.setFolderId(UUID.randomUUID().toString());
			f.setFolderCreationDate(ServerTimeUtil.accurateToDay());
			if (account != null) {
				f.setFolderCreator(account);
			} else {
				f.setFolderCreator("匿名用户");
			}
			f.setFolderParent(parentId);
			int i = 0;
			while (i < 10) {
				try {
					final int r = this.folderRepository.insert(f);
					if (r > 0) {
						if (folderUtil.isValidFolder(f)) {
							this.logUtil.writeCreateFolderEvent(account, ipAddrGetter.getIpAddr(request), f);
							cnfbnr.setResult("success");
							cnfbnr.setNewName(f.getFolderName());
							return gson.toJson(cnfbnr);
						} else {
							break;
						}
					}
					break;
				} catch (Exception e) {
					f.setFolderId(UUID.randomUUID().toString());
					i++;
				}
				if (i >= 10) {
					break;
				}
			}
			retryCount++;
		}
		cnfbnr.setResult("error");
		return gson.toJson(cnfbnr);
	}

	@Override
	public String getFolderCountResult(HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		if (folderId == null || folderId.length() == 0) {
			return AjaxProtocol.ERROR;
		}
		Folder vf = this.folderRepository.selectById(folderId);
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (vf == null || !ConfigurationManager.instance().accessFolder(vf, account)) {
			return AjaxProtocol.ERROR;
		}
		FolderCountResult fcr = new FolderCountResult();
		countFoldersIterator(folderId, account, fcr);
		return gson.toJson(fcr);
	}

	private void countFoldersIterator(String fid, String account, FolderCountResult fcr) {
		final List<Folder> allFolders = this.folderUtil.getAllDescendantFolders(fid);
		final Set<String> accessibleIds = new HashSet<>();
		accessibleIds.add(fid);
		final ConfigurationManager cr = ConfigurationManager.instance();
		long folderSize = 0L;
		for (final Folder f : allFolders) {
			if (accessibleIds.contains(f.getFolderParent()) && cr.accessFolder(f, account)) {
				accessibleIds.add(f.getFolderId());
				folderSize++;
			}
		}
		fcr.setFolderNum(fcr.getFolderNum() + folderSize);
		final List<Node> nodes = this.fileNodeRepository.selectByParentFolderIds(new ArrayList<>(accessibleIds));
		fcr.setFileNum(fcr.getFileNum() + nodes.size());
		long fileSize = nodes.stream()
				.mapToLong(n -> Long.parseLong(n.getFileSize()))
				.sum();
		fcr.setTotalSize(fcr.getTotalSize() + fileSize);
	}

}
