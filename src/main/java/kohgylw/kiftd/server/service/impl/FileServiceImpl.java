package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;

import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import jakarta.annotation.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.listener.ServerInitListener;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.pojo.CheckImportFolderRespons;
import kohgylw.kiftd.server.pojo.CheckUploadFilesRespons;

import org.springframework.web.multipart.*;

import jakarta.servlet.http.*;
import java.io.*;

import kohgylw.kiftd.server.util.*;
import java.util.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * 
 * <h2>文件服务功能实现类</h2>
 * <p>
 * 该类负责对文件相关的服务进行实现操作，例如下载和上传等，各方法功能详见接口定义。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.server.service.FileService
 */
@Service
public class FileServiceImpl extends RangeFileStreamWriter implements FileService {
	private static final String FOLDERS_TOTAL_OUT_OF_LIMIT = "foldersTotalOutOfLimit";
	private static final String FILES_TOTAL_OUT_OF_LIMIT = "filesTotalOutOfLimit";
	private static final String ERROR_PARAMETER = "errorParameter";
	private static final String NO_AUTHORIZED = "noAuthorized";
	private static final String UPLOADSUCCESS = "uploadsuccess";
	private static final String UPLOADERROR = "uploaderror";

	@Resource
	private NodeMapper fm;
	@Resource
	private FolderMapper flm;
	@Resource
	private LogUtil lu;
	@Resource
	private Gson gson;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private FolderUtil fu;
	@Resource
	private IpAddrGetter idg;

	private static final String CONTENT_TYPE = "application/octet-stream";

	public String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String nameList = request.getParameter("namelist");
		final String maxUploadFileSize = request.getParameter("maxSize");
		final String maxUploadFileIndex = request.getParameter("maxFileIndex");
		if (folderId == null || folderId.length() == 0) {
			return ERROR_PARAMETER;
		}
		Folder folder = flm.selectById(folderId);
		if (folder == null) {
			return ERROR_PARAMETER;
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			return NO_AUTHORIZED;
		}
		final List<String> namelistObj = gson.fromJson(nameList, new TypeToken<List<String>>() {
		}.getType());
		CheckUploadFilesRespons cufr = new CheckUploadFilesRespons();
		try {
			long mufs = Long.parseLong(maxUploadFileSize);
			String mfname = namelistObj.get(Integer.parseInt(maxUploadFileIndex));
			long pMaxUploadSize = ConfigureReader.instance().getUploadFileSize(account);
			if (pMaxUploadSize >= 0) {
				if (mufs > pMaxUploadSize) {
					cufr.setCheckResult("fileTooLarge");
					cufr.setMaxUploadFileSize(formatMaxUploadFileSize(pMaxUploadSize));
					cufr.setOverSizeFile(mfname);
					return gson.toJson(cufr);
				}
			}
		} catch (Exception e) {
			return ERROR_PARAMETER;
		}
		final List<String> pereFileNameList = new ArrayList<>();
		for (final String fileName : namelistObj) {
			if (folderId == null || folderId.length() <= 0 || fileName == null || fileName.length() <= 0) {
				return ERROR_PARAMETER;
			}
			final List<Node> files = this.fm.queryByParentFolderId(folderId);
			if (files.stream().parallel().anyMatch((n) -> n.getFileName().equals(fileName))) {
				pereFileNameList.add(fileName);
			}
		}
		long estimatedTotal = fm.selectCount(Wrappers.<Node>lambdaQuery().eq(Node::getFileParentFolder, folderId)) - pereFileNameList.size() + namelistObj.size();
		if (estimatedTotal > FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER || estimatedTotal < 0) {
			return "filesTotalOutOfLimit";
		}
		if (pereFileNameList.size() > 0) {
			cufr.setCheckResult("hasExistsNames");
			cufr.setPereFileNameList(pereFileNameList);
		} else {
			cufr.setCheckResult("permitUpload");
			cufr.setPereFileNameList(new ArrayList<String>());
		}
		return gson.toJson(cufr);
	}

	private String formatMaxUploadFileSize(long size) {
		double result = (double) size;
		String unit = "B";
		if (size <= 0) {
			return "\u8bbe\u7f6e\u65e0\u6548\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458";
		}
		if (size >= 1024 && size < 1048576) {
			result = (double) size / 1024;
			unit = "KB";
		} else if (size >= 1048576 && size < 1073741824) {
			result = (double) size / 1048576;
			unit = "MB";
		} else if (size >= 1073741824) {
			result = (double) size / 1073741824;
			unit = "GB";
		}
		return String.format("%.1f", result) + " " + unit;
	}

	public String doUploadFile(final HttpServletRequest request, final HttpServletResponse response,
			final MultipartFile file) {
		String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String fname = request.getParameter("fname");
		final String originalFileName = (fname != null ? fname : file.getOriginalFilename());
		String fileName = originalFileName;
		final String repeType = request.getParameter("repeType");
		if (folderId == null || folderId.length() <= 0 || originalFileName == null || originalFileName.length() <= 0) {
			return UPLOADERROR;
		}
		Folder folder = flm.selectById(folderId);
		if (folder == null) {
			return UPLOADERROR;
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			return UPLOADERROR;
		}
		long mufs = ConfigureReader.instance().getUploadFileSize(account);
		if (mufs >= 0 && file.getSize() > mufs) {
			return UPLOADERROR;
		}
		final List<Node> nodes = this.fm.queryByParentFolderId(folderId);
		if (nodes.stream().anyMatch((e) -> e.getFileName().equals(originalFileName))) {
			if (repeType != null) {
				switch (repeType) {
				case "skip":
					return UPLOADSUCCESS;
				case "cover":
					if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							fu.getAllFoldersId(folderId))) {
						return UPLOADERROR;
					}
					for (Node f : nodes) {
						if (f.getFileName().equals(originalFileName)) {
							if (!fbu.deleteNode(f)) {
								return UPLOADERROR;
							}
						}
					}
					break;
				case "both":
					fileName = FileNodeUtil.getNewNodeName(originalFileName, nodes);
					break;
				default:
					return UPLOADERROR;
				}
			} else {
				return UPLOADERROR;
			}
		}
		if (fm.selectCount(Wrappers.<Node>lambdaQuery().eq(Node::getFileParentFolder, folderId)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return FILES_TOTAL_OUT_OF_LIMIT;
		}
		final File block = this.fbu.saveToFileBlocks(file);
		if (block == null) {
			return UPLOADERROR;
		}
		final String fsize = this.fbu.getFileSize(file.getSize());
		Node newNode = fbu.insertNewNode(fileName, account, block.getName(), fsize, folderId);
		if (newNode != null) {
			this.lu.writeUploadFileEvent(request, newNode, account);
			return UPLOADSUCCESS;
		} else {
			block.delete();
			return UPLOADERROR;
		}
	}

	public String deleteFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fileId == null || fileId.length() <= 0) {
			return ERROR_PARAMETER;
		}
		final Node node = this.fm.selectById(fileId);
		if (node == null) {
			return "deleteFileSuccess";
		}
		final Folder f = this.flm.selectById(node.getFileParentFolder());
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				fu.getAllFoldersId(node.getFileParentFolder()))
				|| !ConfigureReader.instance().accessFolder(f, account)) {
			return NO_AUTHORIZED;
		}
		if (this.fbu.deleteNode(node)) {
			this.lu.writeDeleteFileEvent(request, node);
			return "deleteFileSuccess";
		}
		return "cannotDeleteFile";
	}

	public void doDownloadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String fileId = request.getParameter("fileId");
		if (fileId != null) {
			final Node f = this.fm.selectById(fileId);
			if (f != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(f.getFileParentFolder()))) {
					Folder folder = flm.selectById(f.getFileParentFolder());
					if (folder != null && ConfigureReader.instance().accessFolder(folder, account)) {
						final File fo = this.fbu.getFileFromBlocks(f);
						final String ip = idg.getIpAddr(request);
						final String range = request.getHeader("Range");
						if (fo != null) {
							int status = writeRangeFileStream(request, response, fo, f.getFileName(), CONTENT_TYPE,
									ConfigureReader.instance().getDownloadMaxRate(account), fbu.getETag(fo), true);
							if (status == HttpServletResponse.SC_OK
									|| (range != null && range.startsWith("bytes=0-"))) {
								this.lu.writeDownloadFileEvent(account, ip, f);
							}
							return;
						}
					}
				}
			}
		}
		try {
			response.sendError(404);
		} catch (IOException e) {
			this.lu.writeException(e);
		}
	}

	public String doRenameFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String newFileName = request.getParameter("newFileName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fileId == null || fileId.length() <= 0 || newFileName == null || newFileName.length() <= 0) {
			return ERROR_PARAMETER;
		}
		if (!TextFormateUtil.instance().matcherFileName(newFileName) || newFileName.indexOf(".") == 0) {
			return ERROR_PARAMETER;
		}
		final Node file = this.fm.selectById(fileId);
		if (file == null) {
			return ERROR_PARAMETER;
		}
		final Folder folder = flm.selectById(file.getFileParentFolder());
		if (!ConfigureReader.instance().accessFolder(folder, account)) {
			return NO_AUTHORIZED;
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER,
				fu.getAllFoldersId(file.getFileParentFolder()))) {
			return NO_AUTHORIZED;
		}
		if (!file.getFileName().equals(newFileName)) {
			if (fm.queryBySomeFolder(fileId).stream().anyMatch((e) -> e.getFileName().equals(newFileName))) {
				return "nameOccupied";
			}
			if (fm.update(null, Wrappers.<Node>lambdaUpdate()
					.set(Node::getFileName, newFileName)
					.eq(Node::getFileId, fileId)) == 0) {
				return "cannotRenameFile";
			}
		}
		this.lu.writeRenameFileEvent(account, idg.getIpAddr(request), file.getFileParentFolder(), file.getFileName(),
				newFileName);
		return "renameFileSuccess";
	}

	public String deleteCheckedFiles(final HttpServletRequest request) {
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		try {
			final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
			}.getType());
			for (final String fileId : idList) {
				if (fileId == null || fileId.length() == 0) {
					return ERROR_PARAMETER;
				}
				final Node file = this.fm.selectById(fileId);
				if (file == null) {
					continue;
				}
				final Folder folder = flm.selectById(file.getFileParentFolder());
				if (folder == null || !ConfigureReader.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						fu.getAllFoldersId(file.getFileParentFolder()))) {
					return NO_AUTHORIZED;
				}
				if (!this.fbu.deleteNode(file)) {
					return "cannotDeleteFile";
				}
				this.lu.writeDeleteFileEvent(request, file);
			}
			final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
			}.getType());
			for (String fid : fidList) {
				Folder folder = flm.selectById(fid);
				if (folder == null) {
					continue;
				}
				if (!ConfigureReader.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						fu.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				final List<Folder> l = this.fu.getParentList(fid);
				if (flm.deleteById(fid) <= 0) {
					return "cannotDeleteFile";
				} else {
					fu.deleteAllChildFolder(fid);
					this.lu.writeDeleteFolderEvent(request, folder, l);
				}
			}
			if (fidList.size() > 0) {
				ServerInitListener.needCheck = true;
			}
			return "deleteFileSuccess";
		} catch (Exception e) {
			return ERROR_PARAMETER;
		}
	}

	public String downloadCheckedFiles(final HttpServletRequest request) {
		if (ConfigureReader.instance().isEnableDownloadByZip()) {
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			final String strIdList = request.getParameter("strIdList");
			final String strFidList = request.getParameter("strFidList");
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				if (idList.size() > 0 || fidList.size() > 0) {
					final String zipname = this.fbu.createZip(idList, fidList, account);
					this.lu.writeDownloadCheckedFileEvent(request, idList, fidList);
					return zipname;
				}
			} catch (Exception ex) {
				lu.writeException(ex);
			}
		}
		return "ERROR";
	}

	public void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		final String zipname = request.getParameter("zipId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (zipname != null && !zipname.equals("ERROR") && !zipname.contains("..")
				&& !zipname.contains("/") && !zipname.contains("\\")) {
			final String tfPath = ConfigureReader.instance().getTemporaryfilePath();
			final File zip = new File(tfPath, zipname);
			String fname = "kiftd_" + ServerTimeUtil.accurateToDay() + "_\u6253\u5305\u4e0b\u8f7d.zip";
			if (zip.exists()) {
				writeRangeFileStream(request, response, zip, fname, CONTENT_TYPE,
						ConfigureReader.instance().getDownloadMaxRate(account), fbu.getETag(zip), true);
				zip.delete();
			}
		}
	}

	public String getPackTime(final HttpServletRequest request) {
		if (ConfigureReader.instance().isEnableDownloadByZip()) {
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			final String strIdList = request.getParameter("strIdList");
			final String strFidList = request.getParameter("strFidList");
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				for (String fid : fidList) {
					countFolderFilesId(account, fid, idList);
				}
				long packTime = 0L;
				for (final String fid : idList) {
					final Node n = this.fm.selectById(fid);
					if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
							fu.getAllFoldersId(n.getFileParentFolder()))
							&& ConfigureReader.instance().accessFolder(flm.selectById(n.getFileParentFolder()),
									account)) {
						final File f = fbu.getFileFromBlocks(n);
						if (f != null && f.exists()) {
							packTime += f.length() / 25000000L;
						}
					}
				}
				if (packTime < 4L) {
					return "\u9a6c\u4e0a\u5b8c\u6210";
				}
				if (packTime >= 4L && packTime < 10L) {
					return "\u5927\u7ea610\u79d2";
				}
				if (packTime >= 10L && packTime < 35L) {
					return "\u4e0d\u5230\u534a\u5206\u949f";
				}
				if (packTime >= 35L && packTime < 65L) {
					return "\u5927\u7ea61\u5206\u949f";
				}
				if (packTime >= 65L) {
					return "\u8d85\u8fc7" + packTime / 60L
							+ "\u5206\u949f\uff0c\u8017\u65f6\u8f83\u957f\uff0c\u5efa\u8bae\u76f4\u63a5\u4e0b\u8f7d";
				}
			} catch (Exception ex) {
				lu.writeException(ex);
			}
		}
		return "0";
	}

	private void countFolderFilesId(String account, String fid, List<String> idList) {
		Folder f = flm.selectById(fid);
		if (ConfigureReader.instance().accessFolder(f, account)) {
			try {
				idList.addAll(Arrays.asList(fm.queryByParentFolderId(fid).stream().map((e) -> e.getFileId())
						.toArray(String[]::new)));
				List<Folder> cFolders = flm.queryByParentId(fid);
				for (Folder cFolder : cFolders) {
					countFolderFilesId(account, cFolder.getFolderId(), idList);
				}
			} catch (Exception e2) {
				this.lu.writeException(e2);
			}
		}
	}

	@Override
	public String doMoveFiles(HttpServletRequest request) {
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String strOptMap = request.getParameter("strOptMap");
		final String locationpath = request.getParameter("locationpath");
		final String method = request.getParameter("method");
		boolean isCopy = "COPY".equals(method);
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		Folder targetFolder = flm.selectById(locationpath);
		if (targetFolder == null) {
			return ERROR_PARAMETER;
		}
		if (!ConfigureReader.instance().accessFolder(targetFolder, account)) {
			return NO_AUTHORIZED;
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES, fu.getAllFoldersId(locationpath))) {
			return NO_AUTHORIZED;
		}
		try {
			final Map<String, String> optMap = gson.fromJson(strOptMap, new TypeToken<Map<String, String>>() {
			}.getType());
			final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
			}.getType());
			final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
			}.getType());
			if (fidList.size() > 0 && !ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
					fu.getAllFoldersId(locationpath))) {
				return NO_AUTHORIZED;
			}
			for (final String id : idList) {
				if (id == null || id.length() <= 0) {
					return ERROR_PARAMETER;
				}
				final Node node = this.fm.selectById(id);
				if (node == null) {
					return ERROR_PARAMETER;
				}
				if (node.getFileParentFolder().equals(locationpath) && !isCopy) {
					continue;
				}
				if (!ConfigureReader.instance().accessFolder(flm.selectById(node.getFileParentFolder()), account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
						fu.getAllFoldersId(node.getFileParentFolder()))) {
					return NO_AUTHORIZED;
				}
				if (!isCopy && !ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						fu.getAllFoldersId(node.getFileParentFolder()))) {
					return NO_AUTHORIZED;
				}
				String originPath = fbu.getNodePath(node);
				String ip = idg.getIpAddr(request);
				if (fm.queryByParentFolderId(locationpath).stream()
						.anyMatch((e) -> e.getFileName().equals(node.getFileName()))) {
					if (optMap.get(id) == null) {
						return ERROR_PARAMETER;
					}
					switch (optMap.get(id)) {
					case "cover":
						if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								fu.getAllFoldersId(locationpath))) {
							return NO_AUTHORIZED;
						}
						Node n = fm.queryByParentFolderId(locationpath).stream()
								.filter((e) -> e.getFileName().equals(node.getFileName())).findFirst().orElse(null);
						if (n == null) {
							continue;
						}
						if (n.getFileId().equals(node.getFileId())) {
							continue;
						}
						if (fbu.deleteNode(n)) {
							if (isCopy) {
								Node copyNode = fbu.insertNewNode(node.getFileName(), account, node.getFilePath(),
										node.getFileSize(), locationpath);
								if (copyNode == null) {
									return "cannotMoveFiles";
								}
								this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(copyNode), isCopy);
							} else {
								node.setFileParentFolder(locationpath);
								if (this.fm.updateById(node) <= 0) {
									return "cannotMoveFiles";
								}
								this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(node), isCopy);
							}
						} else {
							return "cannotMoveFiles";
						}
						break;
					case "both":
						if (fm.selectCount(Wrappers.<Node>lambdaQuery().eq(Node::getFileParentFolder, locationpath)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
							return FILES_TOTAL_OUT_OF_LIMIT;
						}
						if (isCopy) {
							Node copyNode = fbu.insertNewNode(
									FileNodeUtil.getNewNodeName(node.getFileName(),
											fm.queryByParentFolderId(locationpath)),
									account, node.getFilePath(), node.getFileSize(), locationpath);
							if (copyNode == null) {
								return "cannotMoveFiles";
							}
							this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(copyNode), isCopy);
						} else {
							node.setFileName(FileNodeUtil.getNewNodeName(node.getFileName(),
									fm.queryByParentFolderId(locationpath)));
							node.setFileParentFolder(locationpath);
							if (fm.updateById(node) <= 0) {
								return "cannotMoveFiles";
							}
							this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(node), isCopy);
						}
						break;
					case "skip":
						break;
					default:
						return ERROR_PARAMETER;
					}
				} else {
					if (fm.selectCount(Wrappers.<Node>lambdaQuery().eq(Node::getFileParentFolder, locationpath)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
						return FILES_TOTAL_OUT_OF_LIMIT;
					}
					if (isCopy) {
						Node newNode = fbu.insertNewNode(node.getFileName(), account, node.getFilePath(),
								node.getFileSize(), locationpath);
						if (newNode == null) {
							return "cannotMoveFiles";
						}
						this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(newNode), isCopy);
					} else {
						node.setFileParentFolder(locationpath);
						if (this.fm.updateById(node) <= 0) {
							return "cannotMoveFiles";
						}
						this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(node), isCopy);
					}
				}
			}
			for (final String fid : fidList) {
				if (fid == null || fid.length() <= 0) {
					return ERROR_PARAMETER;
				}
				final Folder folder = this.flm.selectById(fid);
				if (folder == null) {
					return ERROR_PARAMETER;
				}
				if (folder.getFolderParent().equals(locationpath) && !isCopy) {
					continue;
				}
				if (!ConfigureReader.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
						fu.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				if (!isCopy && !ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						fu.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				if (!isCopy) {
					if (fid.equals(locationpath) || fu.getParentList(locationpath).stream()
							.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
						return ERROR_PARAMETER;
					}
				}
				String originPath = fu.getFolderPath(folder);
				String ip = idg.getIpAddr(request);
				if (flm.queryByParentId(locationpath).stream()
						.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
					if (optMap.get(fid) == null) {
						return ERROR_PARAMETER;
					}
					switch (optMap.get(fid)) {
					case "cover":
						if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								fu.getAllFoldersId(locationpath))) {
							return NO_AUTHORIZED;
						}
						Folder f = flm.queryByParentId(locationpath).stream()
								.filter((e) -> e.getFolderName().equals(folder.getFolderName())).findFirst().orElse(null);
						if (f == null) {
							break;
						}
						if (flm.deleteById(f.getFolderId()) > 0) {
							if (isCopy) {
								Folder newFolder = fu.copyFolderByNewNameToPath(folder, account, targetFolder, null);
								fu.deleteAllChildFolder(f.getFolderId());
								if (newFolder != null) {
									this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(newFolder),
											isCopy);
									break;
								}
							} else {
								fu.deleteAllChildFolder(f.getFolderId());
								folder.setFolderParent(locationpath);
								boolean needChangeChildsConstranint = false;
								if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
									folder.setFolderConstraint(targetFolder.getFolderConstraint());
									needChangeChildsConstranint = true;
								}
								if (this.flm.updateById(folder) > 0) {
									if (needChangeChildsConstranint) {
										fu.changeChildFolderConstraint(folder.getFolderId(),
												targetFolder.getFolderConstraint());
									}
									this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(folder),
											isCopy);
									break;
								}
							}
						}
						return "cannotMoveFiles";
					case "both":
						if (flm.selectCount(Wrappers.<Folder>lambdaQuery().eq(Folder::getFolderParent, locationpath)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
							return FOLDERS_TOTAL_OUT_OF_LIMIT;
						}
						if (isCopy) {
							Folder newFolder = fu.copyFolderByNewNameToPath(folder, account, targetFolder, FileNodeUtil
									.getNewFolderName(folder.getFolderName(), flm.queryByParentId(locationpath)));
							if (newFolder == null) {
								return "cannotMoveFiles";
							}
							this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(newFolder), isCopy);
						} else {
							folder.setFolderParent(locationpath);
							folder.setFolderName(FileNodeUtil.getNewFolderName(folder.getFolderName(),
									flm.queryByParentId(locationpath)));
							boolean needChangeChildsConstranint = false;
							if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
								folder.setFolderConstraint(targetFolder.getFolderConstraint());
								needChangeChildsConstranint = true;
							}
							if (this.flm.updateById(folder) <= 0) {
								return "cannotMoveFiles";
							}
							if (needChangeChildsConstranint) {
								fu.changeChildFolderConstraint(folder.getFolderId(),
										targetFolder.getFolderConstraint());
							}
							this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(folder), isCopy);
						}
						break;
					case "skip":
						break;
					default:
						return ERROR_PARAMETER;
					}
				} else {
					if (flm.selectCount(Wrappers.<Folder>lambdaQuery().eq(Folder::getFolderParent, locationpath)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
						return FOLDERS_TOTAL_OUT_OF_LIMIT;
					}
					if (isCopy) {
						Folder newFolder = fu.copyFolderByNewNameToPath(folder, account, targetFolder, null);
						if (newFolder == null) {
							return "cannotMoveFiles";
						}
						this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(newFolder), isCopy);
					} else {
						folder.setFolderParent(locationpath);
						boolean needChangeChildsConstranint = false;
						if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
							folder.setFolderConstraint(targetFolder.getFolderConstraint());
							needChangeChildsConstranint = true;
						}
						if (this.flm.updateById(folder) <= 0) {
							return "cannotMoveFiles";
						}
						if (needChangeChildsConstranint) {
							fu.changeChildFolderConstraint(folder.getFolderId(), targetFolder.getFolderConstraint());
						}
						this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(folder), isCopy);
					}
				}
			}
			if (fidList.size() > 0) {
				ServerInitListener.needCheck = true;
			}
			return "moveFilesSuccess";
		} catch (Exception e) {
			return ERROR_PARAMETER;
		}
	}

	@Override
	public String confirmMoveFiles(HttpServletRequest request) {
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String locationpath = request.getParameter("locationpath");
		final String method = request.getParameter("method");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		boolean isCopy = "COPY".equals(method);
		Folder targetFolder = flm.selectById(locationpath);
		int needMovefilesCount = 0;
		int needMoveFoldersCount = 0;
		if (ConfigureReader.instance().accessFolder(targetFolder, account) && ConfigureReader.instance()
				.authorized(account, AccountAuth.MOVE_FILES, fu.getAllFoldersId(locationpath))) {
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				if (fidList.size() > 0 && !ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						fu.getAllFoldersId(locationpath))) {
					return NO_AUTHORIZED;
				}
				List<Node> repeNodes = new ArrayList<>();
				List<Folder> repeFolders = new ArrayList<>();
				for (final String fileId : idList) {
					if (fileId == null || fileId.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Node node = this.fm.selectById(fileId);
					if (node == null) {
						return ERROR_PARAMETER;
					}
					if (node.getFileParentFolder().equals(locationpath) && !isCopy) {
						continue;
					}
					if (!ConfigureReader.instance().accessFolder(flm.selectById(node.getFileParentFolder()), account)) {
						return NO_AUTHORIZED;
					}
					if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
							fu.getAllFoldersId(node.getFileParentFolder()))) {
						return NO_AUTHORIZED;
					}
					if (!isCopy && !ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							fu.getAllFoldersId(node.getFileParentFolder()))) {
						return NO_AUTHORIZED;
					}
					if (fm.queryByParentFolderId(locationpath).stream()
							.anyMatch((e) -> e.getFileName().equals(node.getFileName()))) {
						repeNodes.add(node);
					} else {
						needMovefilesCount++;
					}
				}
				for (final String folderId : fidList) {
					if (folderId == null || folderId.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Folder folder = this.flm.selectById(folderId);
					if (folder == null) {
						return ERROR_PARAMETER;
					}
					if (folder.getFolderParent().equals(locationpath) && !isCopy) {
						continue;
					}
					if (!ConfigureReader.instance().accessFolder(folder, account)) {
						return NO_AUTHORIZED;
					}
					if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
							fu.getAllFoldersId(folder.getFolderParent()))) {
						return NO_AUTHORIZED;
					}
					if (!isCopy && !ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							fu.getAllFoldersId(folder.getFolderParent()))) {
						return NO_AUTHORIZED;
					}
					if (!isCopy) {
						if (folderId.equals(locationpath) || fu.getParentList(locationpath).stream()
								.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
							return "CANT_MOVE_TO_INSIDE:" + folder.getFolderName();
						}
					}
					if (flm.queryByParentId(locationpath).stream()
							.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
						repeFolders.add(folder);
					} else {
						needMoveFoldersCount++;
					}
				}
				long estimateFilesTotal = fm.selectCount(Wrappers.<Node>lambdaQuery().eq(Node::getFileParentFolder, locationpath)) + needMovefilesCount;
				if (estimateFilesTotal > FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER || estimateFilesTotal < 0) {
					return FILES_TOTAL_OUT_OF_LIMIT;
				}
				long estimateFoldersTotal = flm.selectCount(Wrappers.<Folder>lambdaQuery().eq(Folder::getFolderParent, locationpath)) + needMoveFoldersCount;
				if (estimateFoldersTotal > FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER || estimateFoldersTotal < 0) {
					return FOLDERS_TOTAL_OUT_OF_LIMIT;
				}
				if (repeNodes.size() > 0 || repeFolders.size() > 0) {
					Map<String, List<? extends Object>> repeMap = new HashMap<>();
					repeMap.put("repeFolders", repeFolders);
					repeMap.put("repeNodes", repeNodes);
					return "duplicationFileName:" + gson.toJson(repeMap);
				}
				return "confirmMoveFiles";
			} catch (Exception e) {
				return ERROR_PARAMETER;
			}
		}
		return NO_AUTHORIZED;
	}

	@Override
	public String checkImportFolder(HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String folderName = request.getParameter("folderName");
		final String maxUploadFileSize = request.getParameter("maxSize");
		CheckImportFolderRespons cifr = new CheckImportFolderRespons();
		if (folderName == null || folderName.length() == 0) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		if (folderId == null || folderId.length() == 0) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		Folder folder = flm.selectById(folderId);
		if (folder == null) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			cifr.setResult(NO_AUTHORIZED);
			return gson.toJson(cifr);
		}
		try {
			long mufs = Long.parseLong(maxUploadFileSize);
			long pMaxUploadSize = ConfigureReader.instance().getUploadFileSize(account);
			if (pMaxUploadSize >= 0) {
				if (mufs > pMaxUploadSize) {
					cifr.setResult("fileOverSize");
					cifr.setMaxSize(formatMaxUploadFileSize(ConfigureReader.instance().getUploadFileSize(account)));
					return gson.toJson(cifr);
				}
			}
		} catch (Exception e) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		final List<Folder> folders = flm.queryByParentId(folderId);
		try {
			Folder testFolder = folders.stream().parallel().filter((n) -> n.getFolderName().equals(folderName))
					.findAny().get();
			if (ConfigureReader.instance().accessFolder(testFolder, account) && ConfigureReader.instance()
					.authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER, fu.getAllFoldersId(folderId))) {
				cifr.setResult("repeatFolder_coverOrBoth");
			} else {
				cifr.setResult("repeatFolder_Both");
			}
			return gson.toJson(cifr);
		} catch (NoSuchElementException e) {
			if (flm.selectCount(Wrappers.<Folder>lambdaQuery().eq(Folder::getFolderParent, folderId)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
				cifr.setResult(FOLDERS_TOTAL_OUT_OF_LIMIT);
			} else {
				cifr.setResult("permitUpload");
			}
			return gson.toJson(cifr);
		}
	}

	@Override
	public String doImportFolder(HttpServletRequest request, MultipartFile file) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		String folderId = request.getParameter("folderId");
		final String originalFileName = request.getParameter("originalFileName");
		String folderConstraint = request.getParameter("folderConstraint");
		String newFolderName = request.getParameter("newFolderName");
		if (folderId == null || folderId.length() <= 0 || originalFileName == null || originalFileName.length() <= 0) {
			return UPLOADERROR;
		}
		Folder folder = flm.selectById(folderId);
		if (folder == null) {
			return UPLOADERROR;
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			return UPLOADERROR;
		}
		long mufs = ConfigureReader.instance().getUploadFileSize(account);
		if (mufs >= 0 && file.getSize() > mufs) {
			return UPLOADERROR;
		}
		int pc = folder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int ifc = Integer.parseInt(folderConstraint);
				if (ifc != 0 && account == null) {
					return UPLOADERROR;
				}
				if (ifc < pc) {
					return UPLOADERROR;
				}
			} catch (Exception e) {
				return UPLOADERROR;
			}
		} else {
			return UPLOADERROR;
		}
		String[] paths = getParentPath(originalFileName);
		if (paths.length == 0) {
			return UPLOADERROR;
		}
		if (newFolderName != null && newFolderName.length() > 0) {
			paths[0] = newFolderName;
		}
		for (String pName : paths) {
			Folder newFolder;
			try {
				newFolder = fu.createNewFolder(folderId, account, pName, folderConstraint);
			} catch (FoldersTotalOutOfLimitException e1) {
				return FOLDERS_TOTAL_OUT_OF_LIMIT;
			}
			if (newFolder == null) {
				Map<String, String> key = new HashMap<String, String>();
				key.put("parentId", folderId);
				key.put("folderName", pName);
				Folder target = flm.queryByParentIdAndFolderName(key);
				if (target != null) {
					folderId = target.getFolderId();
				} else {
					return UPLOADERROR;
				}
			} else {
				if (!fu.isValidFolder(newFolder)) {
					return UPLOADERROR;
				}
				folderId = newFolder.getFolderId();
			}
		}
		String fileName = getFileNameFormPath(originalFileName);
		final List<Node> files = this.fm.queryByParentFolderId(folderId);
		if (files.stream().anyMatch((e) -> e.getFileName().equals(fileName))) {
			return UPLOADERROR;
		}
		if (fm.selectCount(Wrappers.<Node>lambdaQuery().eq(Node::getFileParentFolder, folderId)) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return FILES_TOTAL_OUT_OF_LIMIT;
		}
		final File block = this.fbu.saveToFileBlocks(file);
		if (block == null) {
			return UPLOADERROR;
		}
		final String fsize = this.fbu.getFileSize(file.getSize());
		Node newNode = fbu.insertNewNode(fileName, account, block.getName(), fsize, folderId);
		if (newNode != null) {
			this.lu.writeUploadFileEvent(request, newNode, account);
			return UPLOADSUCCESS;
		} else {
			block.delete();
			return UPLOADERROR;
		}
	}

	private String[] getParentPath(String path) {
		if (path != null) {
			String[] paths = path.split("/");
			List<String> result = new ArrayList<String>();
			for (int i = 0; i < paths.length - 1; i++) {
				if (paths[i].length() > 0) {
					result.add(paths[i]);
				}
			}
			return result.toArray(new String[0]);
		}
		return new String[0];
	}

	private String getFileNameFormPath(String path) {
		if (path != null) {
			String[] paths = path.split("/");
			if (paths.length > 0) {
				return paths[paths.length - 1];
			}
		}
		return null;
	}

}