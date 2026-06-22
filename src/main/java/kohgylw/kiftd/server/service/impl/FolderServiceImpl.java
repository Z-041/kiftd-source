package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;

import com.google.gson.Gson;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import kohgylw.kiftd.server.mapper.*;
import jakarta.annotation.*;
import jakarta.servlet.http.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.listener.ServerInitListener;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.pojo.CreateNewFolderByNameRespons;
import kohgylw.kiftd.server.pojo.FolderCountResult;
import kohgylw.kiftd.server.util.*;

import java.util.*;

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
 * @see kohgylw.kiftd.server.service.FolderService
 */
@Service
public class FolderServiceImpl implements FolderService {

	@Resource
	private FolderMapper fm;
	@Resource
	private NodeMapper nm;
	@Resource
	private FolderUtil fu;
	@Resource
	private LogUtil lu;
	@Resource
	private Gson gson;
	@Resource
	private IpAddrGetter idg;

	public String newFolder(final HttpServletRequest request) {
		final String parentId = request.getParameter("parentId");
		final String folderName = request.getParameter("folderName");
		final String folderConstraint = request.getParameter("folderConstraint");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (parentId == null || folderName == null || parentId.length() <= 0 || folderName.length() <= 0) {
			return "errorParameter";
		}
		if (!TextFormateUtil.instance().matcherFolderName(folderName) || folderName.indexOf(".") == 0) {
			return "errorParameter";
		}
		final Folder parentFolder = this.fm.selectById(parentId);
		if (parentFolder == null || !ConfigureReader.instance().accessFolder(parentFolder, account)) {
			return "errorParameter";
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
				fu.getAllFoldersId(parentId))) {
			return "noAuthorized";
		}
		if (fm.queryByParentId(parentId).stream().anyMatch((e) -> e.getFolderName().equals(folderName))) {
			return "nameOccupied";
		}
		if (fm.selectCount(Wrappers.<Folder>lambdaQuery().eq(Folder::getFolderParent, parentId)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return "foldersTotalOutOfLimit";
		}
		Folder f = new Folder();
		int pc = parentFolder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int ifc = Integer.parseInt(folderConstraint);
				if (ifc != 0 && account == null) {
					return "errorParameter";
				}
				if (ifc < pc) {
					return "errorParameter";
				} else {
					f.setFolderConstraint(ifc);
				}
			} catch (Exception e) {
				return "errorParameter";
			}
		} else {
			return "errorParameter";
		}
		f.setFolderId(UUID.randomUUID().toString());
		f.setFolderName(folderName);
		f.setFolderCreationDate(ServerTimeUtil.accurateToDay());
		if (account != null) {
			f.setFolderCreator(account);
		} else {
			f.setFolderCreator("匿名用户");
		}
		f.setFolderParent(parentId);
		int i = 0;
		while (true) {
			try {
				final int r = this.fm.insert(f);
				if (r > 0) {
					if (fu.isValidFolder(f)) {
						this.lu.writeCreateFolderEvent(account, idg.getIpAddr(request), f);
						return "createFolderSuccess";
					} else {
						return "cannotCreateFolder";
					}
				}
				break;
			} catch (Exception e) {
				f.setFolderId(UUID.randomUUID().toString());
				this.lu.writeException(e);
				i++;
			}
			if (i >= 10) {
				break;
			}
		}
		return "cannotCreateFolder";
	}

	public String deleteFolder(final HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (folderId == null || folderId.length() == 0 || "root".equals(folderId)) {
			return "errorParameter";
		}
		final Folder folder = this.fm.selectById(folderId);
		if (folder == null) {
			return "deleteFolderSuccess";
		}
		if (!ConfigureReader.instance().accessFolder(folder, account)) {
			return "noAuthorized";
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				fu.getAllFoldersId(folder.getFolderParent()))) {
			return "noAuthorized";
		}
		final List<Folder> l = this.fu.getParentList(folderId);
		if (this.fm.deleteById(folderId) > 0) {
			fu.deleteAllChildFolder(folderId);
			this.lu.writeDeleteFolderEvent(request, folder, l);
			ServerInitListener.needCheck = true;
			return "deleteFolderSuccess";
		}
		return "cannotDeleteFolder";
	}

	public String renameFolder(final HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		final String newName = request.getParameter("newName");
		final String folderConstraint = request.getParameter("folderConstraint");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (folderId == null || folderId.length() == 0 || newName == null || newName.length() == 0
				|| "root".equals(folderId)) {
			return "errorParameter";
		}
		if (!TextFormateUtil.instance().matcherFolderName(newName) || newName.indexOf(".") == 0) {
			return "errorParameter";
		}
		final Folder folder = this.fm.selectById(folderId);
		if (folder == null) {
			return "errorParameter";
		}
		if (!ConfigureReader.instance().accessFolder(folder, account)) {
			return "noAuthorized";
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER,
				fu.getAllFoldersId(folder.getFolderParent()))) {
			return "noAuthorized";
		}
		final Folder parentFolder = this.fm.selectById(folder.getFolderParent());
		int pc = parentFolder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int ifc = Integer.parseInt(folderConstraint);
				if (ifc > 0 && account == null) {
					return "errorParameter";
				}
				if (ifc < pc) {
					return "errorParameter";
				} else {
					fm.update(null, Wrappers.<Folder>lambdaUpdate()
							.set(Folder::getFolderConstraint, ifc)
							.eq(Folder::getFolderId, folderId));
					fu.changeChildFolderConstraint(folderId, ifc);
					if (!folder.getFolderName().equals(newName)) {
						if (fm.queryByParentId(parentFolder.getFolderId()).stream()
							.anyMatch((e) -> e.getFolderName().equals(newName))) {
							return "nameOccupied";
						}
						if (fm.update(null, Wrappers.<Folder>lambdaUpdate()
								.set(Folder::getFolderName, newName)
								.eq(Folder::getFolderId, folderId)) == 0) {
							return "errorParameter";
						}
					}
					this.lu.writeRenameFolderEvent(account, idg.getIpAddr(request), folder.getFolderId(),
							folder.getFolderName(), newName, folder.getFolderConstraint() + "", folderConstraint);
					return "renameFolderSuccess";
				}
			} catch (Exception e) {
				return "errorParameter";
			}
		} else {
			return "errorParameter";
		}
	}

	@Override
	public String deleteFolderByName(HttpServletRequest request) {
		final String parentId = request.getParameter("parentId");
		final String folderName = request.getParameter("folderName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (parentId == null || parentId.length() == 0) {
			return "deleteError";
		}
		Folder p = fm.selectById(parentId);
		if (p == null) {
			return "deleteError";
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				fu.getAllFoldersId(parentId)) || !ConfigureReader.instance().accessFolder(p, account)) {
			return "deleteError";
		}
		final Folder[] repeatFolders = this.fm.queryByParentId(parentId).stream()
				.filter((f) -> f.getFolderName().equals(folderName))
				.toArray(Folder[]::new);
		for (Folder rf : repeatFolders) {
			if (!ConfigureReader.instance().accessFolder(rf, account)) {
				return "deleteError";
			}
			final List<Folder> l = this.fu.getParentList(rf.getFolderId());
			if (this.fm.deleteById(rf.getFolderId()) > 0) {
				fu.deleteAllChildFolder(rf.getFolderId());
				this.lu.writeDeleteFolderEvent(request, rf, l);
			} else {
				return "deleteError";
			}
		}
		ServerInitListener.needCheck = true;
		return "deleteSuccess";
	}

	@Override
	public String createNewFolderByName(HttpServletRequest request) {
		final String parentId = request.getParameter("parentId");
		final String folderName = request.getParameter("folderName");
		final String folderConstraint = request.getParameter("folderConstraint");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		CreateNewFolderByNameRespons cnfbnr = new CreateNewFolderByNameRespons();
		if (parentId == null || folderName == null || parentId.length() <= 0 || folderName.length() <= 0) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		if (folderName.equals(".") || folderName.equals("..")) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		final Folder parentFolder = this.fm.selectById(parentId);
		if (parentFolder == null || !ConfigureReader.instance().accessFolder(parentFolder, account)) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
				fu.getAllFoldersId(parentId))) {
			cnfbnr.setResult("error");
			return gson.toJson(cnfbnr);
		}
		if (fm.selectCount(Wrappers.<Folder>lambdaQuery().eq(Folder::getFolderParent, parentId)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			cnfbnr.setResult("foldersTotalOutOfLimit");
			return gson.toJson(cnfbnr);
		}
		Folder f = new Folder();
		if (fm.queryByParentId(parentId).stream().anyMatch((e) -> e.getFolderName().equals(folderName))) {
			f.setFolderName(FileNodeUtil.getNewFolderName(folderName, fm.queryByParentId(parentId)));
		} else {
			f.setFolderName(folderName);
		}
		int pc = parentFolder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int ifc = Integer.parseInt(folderConstraint);
				if (ifc != 0 && account == null) {
					cnfbnr.setResult("error");
					return gson.toJson(cnfbnr);
				}
				if (ifc < pc) {
					cnfbnr.setResult("error");
					return gson.toJson(cnfbnr);
				} else {
					f.setFolderConstraint(ifc);
				}
			} catch (Exception e) {
				cnfbnr.setResult("error");
				return gson.toJson(cnfbnr);
			}
		} else {
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
		while (true) {
			try {
				final int r = this.fm.insert(f);
				if (r > 0) {
					if (fu.isValidFolder(f)) {
						this.lu.writeCreateFolderEvent(account, idg.getIpAddr(request), f);
						cnfbnr.setResult("success");
						cnfbnr.setNewName(f.getFolderName());
						return gson.toJson(cnfbnr);
					} else {
						cnfbnr.setResult("error");
						return gson.toJson(cnfbnr);
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
		cnfbnr.setResult("error");
		return gson.toJson(cnfbnr);
	}

	@Override
	public String getFolderCountResult(HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		if (folderId == null || folderId.length() == 0) {
			return "ERROR";
		}
		Folder vf = this.fm.selectById(folderId);
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (vf == null || !ConfigureReader.instance().accessFolder(vf, account)) {
			return "ERROR";
		}
		FolderCountResult fcr = new FolderCountResult();
		countFoldersIterator(folderId, account, fcr);
		return gson.toJson(fcr);
	}

	private void countFoldersIterator(String fid, String account, FolderCountResult fcr) {
		long folderSize = 0L;
		for (Folder f : this.fm.queryByParentId(fid)) {
			if (ConfigureReader.instance().accessFolder(f, account)) {
				folderSize++;
				countFoldersIterator(f.getFolderId(), account, fcr);
			}
		}
		fcr.setFolderNum(fcr.getFolderNum() + folderSize);
		List<Node> nodes = this.nm.queryByParentFolderId(fid);
		fcr.setFileNum(fcr.getFileNum() + nodes.size());
		long fileSize = 0L;
		for (Node n : nodes) {
			fileSize += Long.parseLong(n.getFileSize());
		}
		fcr.setTotalSize(fcr.getTotalSize() + fileSize);
	}

}