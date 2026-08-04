package kohgylw.kiftd.newcore.service.impl;

import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.newcore.service.FileService;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.listener.ServerInitListener;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.CheckImportFolderRespons;
import kohgylw.kiftd.server.pojo.CheckUploadFilesRespons;
import kohgylw.kiftd.server.util.CircuitBreaker;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FileNodeUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;
import kohgylw.kiftd.server.util.RetryUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.util.TextFormateUtil;
import kohgylw.kiftd.util.SizeFormatUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 
 * <h2>文件服务功能实现类</h2>
 * <p>
 * 该类负责对文件相关的服务进行实现操作，例如下载和上传等，各方法功能详见接口定义。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.newcore.service.FileService
 */
@Service
@Primary
public class FileServiceImpl implements FileService {
	private static final String FOLDERS_TOTAL_OUT_OF_LIMIT = "foldersTotalOutOfLimit";
	private static final String FILES_TOTAL_OUT_OF_LIMIT = "filesTotalOutOfLimit";
	private static final String ERROR_PARAMETER = "errorParameter";
	private static final String NO_AUTHORIZED = "noAuthorized";
	private static final String UPLOADSUCCESS = "uploadsuccess";
	private static final String UPLOADERROR = "uploaderror";

	private final FileNodeRepository fileNodeRepository;
	private final FolderRepository folderRepository;
	private final LogUtil logUtil;
	private final Gson gson;
	private final FileBlockUtil fileBlockUtil;
	private final FolderUtil folderUtil;
	private final IpAddrGetter ipAddrGetter;

	private static final String CONTENT_TYPE = "application/octet-stream";

	private CircuitBreaker dbCircuitBreaker;
	private CircuitBreaker fileStorageCircuitBreaker;

	public FileServiceImpl(FileNodeRepository fileNodeRepository, FolderRepository folderRepository,
			LogUtil logUtil, Gson gson, FileBlockUtil fileBlockUtil, FolderUtil folderUtil, IpAddrGetter ipAddrGetter) {
		this.fileNodeRepository = fileNodeRepository;
		this.folderRepository = folderRepository;
		this.logUtil = logUtil;
		this.gson = gson;
		this.fileBlockUtil = fileBlockUtil;
		this.folderUtil = folderUtil;
		this.ipAddrGetter = ipAddrGetter;
	}

	@PostConstruct
	public void initCircuitBreakers() {
		dbCircuitBreaker = new CircuitBreaker("FileService-Database", 5, 30000, 3);
		fileStorageCircuitBreaker = new CircuitBreaker("FileService-FileStorage", 3, 60000, 2);
	}

	@Override
	public String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String nameList = request.getParameter("namelist");
		final String maxUploadFileSize = request.getParameter("maxSize");
		final String maxUploadFileIndex = request.getParameter("maxFileIndex");
		if (folderId == null || folderId.length() == 0) {
			return ERROR_PARAMETER;
		}
		Folder folder = folderRepository.selectById(folderId);
		if (folder == null) {
			return ERROR_PARAMETER;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.UPLOAD_FILES, folderUtil.getAllFoldersId(folderId))
				|| !ConfigurationManager.instance().accessFolder(folder, account)) {
			return NO_AUTHORIZED;
		}
		final List<String> namelistObj = gson.fromJson(nameList, new TypeToken<List<String>>() {
		}.getType());
		CheckUploadFilesRespons cufr = new CheckUploadFilesRespons();
		try {
			long mufs = Long.parseLong(maxUploadFileSize);
			String mfname = namelistObj.get(Integer.parseInt(maxUploadFileIndex));
			long pMaxUploadSize = ConfigurationManager.instance().getUploadFileSize(account);
			if (pMaxUploadSize >= 0) {
				if (mufs > pMaxUploadSize) {
					cufr.setCheckResult("fileTooLarge");
					cufr.setMaxUploadFileSize(SizeFormatUtil.formatFileSize(pMaxUploadSize,
							"\u8bbe\u7f6e\u65e0\u6548\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458"));
					cufr.setOverSizeFile(mfname);
					return gson.toJson(cufr);
				}
			}
		} catch (Exception e) {
			return ERROR_PARAMETER;
		}
		final List<String> pereFileNameList = new ArrayList<>();
		final List<Node> files = this.fileNodeRepository.selectByParentFolderId(folderId);
		final Set<String> existingNames = files == null ? Collections.emptySet()
				: files.stream().map(Node::getFileName).collect(Collectors.toSet());
		for (final String fileName : namelistObj) {
			if (fileName == null || fileName.length() <= 0
					|| !TextFormateUtil.instance().matcherFileName(fileName)) {
				return ERROR_PARAMETER;
			}
			if (existingNames.contains(fileName)) {
				pereFileNameList.add(fileName);
			}
		}
		long estimatedTotal = fileNodeRepository.countByParentFolderId(folderId) - pereFileNameList.size() + namelistObj.size();
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

	@Override
	@Transactional
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
		if (!TextFormateUtil.instance().matcherFileName(originalFileName)) {
			return UPLOADERROR;
		}
		Folder folder = folderRepository.selectById(folderId);
		if (folder == null) {
			return UPLOADERROR;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.UPLOAD_FILES, folderUtil.getAllFoldersId(folderId))
				|| !ConfigurationManager.instance().accessFolder(folder, account)) {
			return UPLOADERROR;
		}
		long mufs = ConfigurationManager.instance().getUploadFileSize(account);
		if (mufs >= 0 && file.getSize() > mufs) {
			return UPLOADERROR;
		}
		List<Node> nodes = this.fileNodeRepository.selectByParentFolderId(folderId);
		if (nodes.stream().anyMatch((e) -> e.getFileName().equals(originalFileName))) {
			if (repeType != null) {
				switch (repeType) {
				case "skip":
					return UPLOADSUCCESS;
				case "cover":
					if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							folderUtil.getAllFoldersId(folderId))) {
						return UPLOADERROR;
					}
					for (Node f : nodes) {
						if (f.getFileName().equals(originalFileName)) {
							if (!fileBlockUtil.deleteNode(f)) {
								return UPLOADERROR;
							}
						}
					}
					nodes = this.fileNodeRepository.selectByParentFolderId(folderId);
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
		if (fileNodeRepository.countByParentFolderId(folderId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return FILES_TOTAL_OUT_OF_LIMIT;
		}
		final File block = RetryUtil.executeWithRetry(
				() -> {
					if (!fileStorageCircuitBreaker.isRequestAllowed()) {
						throw new RuntimeException("文件存储熔断器已打开");
					}
					try {
						File result = this.fileBlockUtil.saveToFileBlocks(file);
						if (result != null) {
							fileStorageCircuitBreaker.recordSuccess();
							return result;
						}
						throw new IOException("文件保存失败");
					} catch (Exception e) {
						fileStorageCircuitBreaker.recordFailure(e);
						throw e;
					}
				},
				"文件保存至存储", 2, 100, 2.0);
		if (block == null) {
			return UPLOADERROR;
		}
		final String fsize = this.fileBlockUtil.getFileSize(file.getSize());
		final String finalFileName = fileName;
		Node newNode = RetryUtil.executeWithRetry(
				() -> {
					if (!dbCircuitBreaker.isRequestAllowed()) {
						throw new RuntimeException("数据库熔断器已打开");
					}
					try {
						Node result = insertNodeWithRetry(finalFileName, account, block.getName(), fsize, folderId, originalFileName);
						if (result != null) {
							dbCircuitBreaker.recordSuccess();
							return result;
						}
						throw new RuntimeException("节点插入失败");
					} catch (Exception e) {
						dbCircuitBreaker.recordFailure(e);
						throw e;
					}
				},
				"数据库节点插入", 2, 100, 2.0);
		if (newNode != null) {
			this.logUtil.writeUploadFileEvent(request, newNode, account);
			return UPLOADSUCCESS;
		} else {
			block.delete();
			return UPLOADERROR;
		}
	}

	private Node insertNodeWithRetry(String fileName, String account, String blockName, String fsize, String folderId, String originalFileName) {
		int retryCount = 0;
		Node newNode = null;
		String currentFileName = fileName;
		while (retryCount < 3) {
			newNode = fileBlockUtil.insertNewNode(currentFileName, account, blockName, fsize, folderId);
			if (newNode != null) {
				break;
			}
			List<Node> currentNodes = this.fileNodeRepository.selectByParentFolderId(folderId);
			final String checkName = currentFileName;
			if (currentNodes.stream().anyMatch((e) -> e.getFileName().equals(checkName))) {
				currentFileName = FileNodeUtil.getNewNodeName(originalFileName, currentNodes);
				retryCount++;
			} else {
				break;
			}
		}
		return newNode;
	}

	@Override
	@Transactional
	public String deleteFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fileId == null || fileId.length() <= 0) {
			return ERROR_PARAMETER;
		}
		final Node node = this.fileNodeRepository.selectById(fileId);
		if (node == null) {
			return "deleteFileSuccess";
		}
		final Folder f = this.folderRepository.selectById(node.getFileParentFolder());
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				folderUtil.getAllFoldersId(node.getFileParentFolder()))
				|| !ConfigurationManager.instance().accessFolder(f, account)) {
			return NO_AUTHORIZED;
		}
		if (this.fileBlockUtil.deleteNode(node)) {
			this.logUtil.writeDeleteFileEvent(request, node);
			return "deleteFileSuccess";
		}
		return "cannotDeleteFile";
	}

	@Override
	public void doDownloadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String fileId = request.getParameter("fileId");
		if (fileId != null) {
			final Node f = this.fileNodeRepository.selectById(fileId);
			if (f != null) {
				if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(f.getFileParentFolder()))) {
					Folder folder = folderRepository.selectById(f.getFileParentFolder());
					if (folder != null && ConfigurationManager.instance().accessFolder(folder, account)) {
						final File fo = this.fileBlockUtil.getFileFromBlocks(f);
						final String ip = ipAddrGetter.getIpAddr(request);
						final String range = request.getHeader("Range");
						if (fo != null) {
							int status = RangeFileStreamWriter.writeRangeFileStream(request, response, fo, f.getFileName(), CONTENT_TYPE,
									ConfigurationManager.instance().getDownloadMaxRate(account), fileBlockUtil.getETag(fo), true);
							if (status == HttpServletResponse.SC_OK
									|| (range != null && range.startsWith("bytes=0-"))) {
								this.logUtil.writeDownloadFileEvent(account, ip, f);
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
			this.logUtil.writeException(e);
		}
	}

	@Override
	@Transactional
	public String doRenameFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String newFileName = request.getParameter("newFileName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fileId == null || fileId.length() <= 0 || newFileName == null || newFileName.length() <= 0) {
			return ERROR_PARAMETER;
		}
		if (!TextFormateUtil.instance().matcherFileName(newFileName)) {
			return ERROR_PARAMETER;
		}
		final Node file = this.fileNodeRepository.selectById(fileId);
		if (file == null) {
			return ERROR_PARAMETER;
		}
		final Folder folder = folderRepository.selectById(file.getFileParentFolder());
		if (!ConfigurationManager.instance().accessFolder(folder, account)) {
			return NO_AUTHORIZED;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER,
				folderUtil.getAllFoldersId(file.getFileParentFolder()))) {
			return NO_AUTHORIZED;
		}
		if (!file.getFileName().equals(newFileName)) {
			int retryCount = 0;
			boolean success = false;
			while (retryCount < 3) {
				if (fileNodeRepository.selectBySomeFolder(fileId).stream().anyMatch((e) -> e.getFileName().equals(newFileName))) {
					return "nameOccupied";
				}
				Node nodeToUpdate = fileNodeRepository.selectById(fileId);
				if (nodeToUpdate != null) {
					nodeToUpdate.setFileName(newFileName);
					if (fileNodeRepository.update(nodeToUpdate) > 0) {
						Node updatedNode = fileNodeRepository.selectById(fileId);
						if (updatedNode != null && newFileName.equals(updatedNode.getFileName())) {
							success = true;
							break;
						}
					}
				}
				retryCount++;
			}
			if (!success) {
				return "cannotRenameFile";
			}
		}
		this.logUtil.writeRenameFileEvent(account, ipAddrGetter.getIpAddr(request), file.getFileParentFolder(), file.getFileName(),
				newFileName);
		return "renameFileSuccess";
	}

	@Override
	@Transactional
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
				final Node file = this.fileNodeRepository.selectById(fileId);
				if (file == null) {
					continue;
				}
				final Folder folder = folderRepository.selectById(file.getFileParentFolder());
				if (folder == null || !ConfigurationManager.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						folderUtil.getAllFoldersId(file.getFileParentFolder()))) {
					return NO_AUTHORIZED;
				}
				if (!this.fileBlockUtil.deleteNode(file)) {
					return "cannotDeleteFile";
				}
				this.logUtil.writeDeleteFileEvent(request, file);
			}
			final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
			}.getType());
			for (String fid : fidList) {
				Folder folder = folderRepository.selectById(fid);
				if (folder == null) {
					continue;
				}
				if (!ConfigurationManager.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						folderUtil.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				final List<Folder> l = this.folderUtil.getParentList(fid);
				if (folderRepository.deleteById(fid) <= 0) {
					return "cannotDeleteFile";
				} else {
					folderUtil.deleteAllChildFolder(fid);
					this.logUtil.writeDeleteFolderEvent(request, folder, l);
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

	@Override
	public String downloadCheckedFiles(final HttpServletRequest request) {
		if (ConfigurationManager.instance().isEnableDownloadByZip()) {
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			final String strIdList = request.getParameter("strIdList");
			final String strFidList = request.getParameter("strFidList");
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				if (idList.size() > 0 || fidList.size() > 0) {
					final String zipname = this.fileBlockUtil.createZip(idList, fidList, account);
					this.logUtil.writeDownloadCheckedFileEvent(request, idList, fidList);
					return zipname;
				}
			} catch (Exception ex) {
				logUtil.writeException(ex);
			}
		}
		return "ERROR";
	}

	@Override
	public void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		final String zipname = request.getParameter("zipId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (zipname != null && !zipname.equals("ERROR") && !zipname.contains("..")
				&& !zipname.contains("/") && !zipname.contains("\\")) {
			final String tfPath = ConfigurationManager.instance().getTemporaryfilePath();
			final File zip = new File(tfPath, zipname);
			String fname = "kiftd_" + ServerTimeUtil.accurateToDay() + "_\u6253\u5305\u4e0b\u8f7d.zip";
			if (zip.exists()) {
				RangeFileStreamWriter.writeRangeFileStream(request, response, zip, fname, CONTENT_TYPE,
						ConfigurationManager.instance().getDownloadMaxRate(account), fileBlockUtil.getETag(zip), true);
				zip.delete();
			}
		}
	}

	@Override
	public String getPackTime(final HttpServletRequest request) {
		if (ConfigurationManager.instance().isEnableDownloadByZip()) {
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
					final Node n = this.fileNodeRepository.selectById(fid);
					if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
							folderUtil.getAllFoldersId(n.getFileParentFolder()))
							&& ConfigurationManager.instance().accessFolder(folderRepository.selectById(n.getFileParentFolder()),
									account)) {
						final File f = fileBlockUtil.getFileFromBlocks(n);
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
				logUtil.writeException(ex);
			}
		}
		return "0";
	}

	private void countFolderFilesId(String account, String fid, List<String> idList) {
		Folder f = folderRepository.selectById(fid);
		if (ConfigurationManager.instance().accessFolder(f, account)) {
			try {
				final List<Folder> allFolders = this.folderUtil.getAllDescendantFolders(fid);
				final Set<String> accessibleIds = new HashSet<>();
				accessibleIds.add(fid);
				for (final Folder cf : allFolders) {
					if (accessibleIds.contains(cf.getFolderParent())
							&& ConfigurationManager.instance().accessFolder(cf, account)) {
						accessibleIds.add(cf.getFolderId());
					}
				}
				final List<Node> files = this.fileNodeRepository.selectByParentFolderIds(new ArrayList<>(accessibleIds));
				idList.addAll(files.stream().map(Node::getFileId).collect(Collectors.toList()));
			} catch (Exception e2) {
				this.logUtil.writeException(e2);
			}
		}
	}

	@Override
	@Transactional
	public String doMoveFiles(HttpServletRequest request) {
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String strOptMap = request.getParameter("strOptMap");
		final String locationpath = request.getParameter("locationpath");
		final String method = request.getParameter("method");
		boolean isCopy = "COPY".equals(method);
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		Folder targetFolder = folderRepository.selectById(locationpath);
		if (targetFolder == null) {
			return ERROR_PARAMETER;
		}
		if (!ConfigurationManager.instance().accessFolder(targetFolder, account)) {
			return NO_AUTHORIZED;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.MOVE_FILES, folderUtil.getAllFoldersId(locationpath))) {
			return NO_AUTHORIZED;
		}
		try {
			final Map<String, String> optMap = gson.fromJson(strOptMap, new TypeToken<Map<String, String>>() {
			}.getType());
			final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
			}.getType());
			final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
			}.getType());
			if (fidList.size() > 0 && !ConfigurationManager.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
					folderUtil.getAllFoldersId(locationpath))) {
				return NO_AUTHORIZED;
			}
			for (final String id : idList) {
				if (id == null || id.length() <= 0) {
					return ERROR_PARAMETER;
				}
				final Node node = this.fileNodeRepository.selectById(id);
				if (node == null) {
					return ERROR_PARAMETER;
				}
				if (node.getFileParentFolder().equals(locationpath) && !isCopy) {
					continue;
				}
				if (!ConfigurationManager.instance().accessFolder(folderRepository.selectById(node.getFileParentFolder()), account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigurationManager.instance().authorized(account, AccountAuth.MOVE_FILES,
						folderUtil.getAllFoldersId(node.getFileParentFolder()))) {
					return NO_AUTHORIZED;
				}
				if (!isCopy && !ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						folderUtil.getAllFoldersId(node.getFileParentFolder()))) {
					return NO_AUTHORIZED;
				}
				String originPath = fileBlockUtil.getNodePath(node);
				String ip = ipAddrGetter.getIpAddr(request);
				if (fileNodeRepository.selectByParentFolderId(locationpath).stream()
						.anyMatch((e) -> e.getFileName().equals(node.getFileName()))) {
					if (optMap.get(id) == null) {
						return ERROR_PARAMETER;
					}
					switch (optMap.get(id)) {
					case "cover":
						if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								folderUtil.getAllFoldersId(locationpath))) {
							return NO_AUTHORIZED;
						}
						Node n = fileNodeRepository.selectByParentFolderId(locationpath).stream()
								.filter((e) -> e.getFileName().equals(node.getFileName())).findFirst().orElse(null);
						if (n == null) {
							continue;
						}
						if (n.getFileId().equals(node.getFileId())) {
							continue;
						}
						if (fileBlockUtil.deleteNode(n)) {
							if (isCopy) {
								Node copyNode = fileBlockUtil.insertNewNode(node.getFileName(), account, node.getFilePath(),
										node.getFileSize(), locationpath);
								if (copyNode == null) {
									return "cannotMoveFiles";
								}
								this.logUtil.writeMoveFileEvent(account, ip, originPath, fileBlockUtil.getNodePath(copyNode), isCopy);
							} else {
								node.setFileParentFolder(locationpath);
								if (this.fileNodeRepository.update(node) <= 0) {
									return "cannotMoveFiles";
								}
								this.logUtil.writeMoveFileEvent(account, ip, originPath, fileBlockUtil.getNodePath(node), isCopy);
							}
						} else {
							return "cannotMoveFiles";
						}
						break;
					case "both":
						if (fileNodeRepository.countByParentFolderId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
							return FILES_TOTAL_OUT_OF_LIMIT;
						}
						if (isCopy) {
							Node copyNode = fileBlockUtil.insertNewNode(
									FileNodeUtil.getNewNodeName(node.getFileName(),
											fileNodeRepository.selectByParentFolderId(locationpath)),
									account, node.getFilePath(), node.getFileSize(), locationpath);
							if (copyNode == null) {
								return "cannotMoveFiles";
							}
							this.logUtil.writeMoveFileEvent(account, ip, originPath, fileBlockUtil.getNodePath(copyNode), isCopy);
						} else {
							node.setFileName(FileNodeUtil.getNewNodeName(node.getFileName(),
									fileNodeRepository.selectByParentFolderId(locationpath)));
							node.setFileParentFolder(locationpath);
							if (fileNodeRepository.update(node) <= 0) {
								return "cannotMoveFiles";
							}
							this.logUtil.writeMoveFileEvent(account, ip, originPath, fileBlockUtil.getNodePath(node), isCopy);
						}
						break;
					case "skip":
						break;
					default:
						return ERROR_PARAMETER;
					}
				} else {
					if (fileNodeRepository.countByParentFolderId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
						return FILES_TOTAL_OUT_OF_LIMIT;
					}
					if (isCopy) {
						Node newNode = fileBlockUtil.insertNewNode(node.getFileName(), account, node.getFilePath(),
								node.getFileSize(), locationpath);
						if (newNode == null) {
							return "cannotMoveFiles";
						}
						this.logUtil.writeMoveFileEvent(account, ip, originPath, fileBlockUtil.getNodePath(newNode), isCopy);
					} else {
						node.setFileParentFolder(locationpath);
						if (this.fileNodeRepository.update(node) <= 0) {
							return "cannotMoveFiles";
						}
						this.logUtil.writeMoveFileEvent(account, ip, originPath, fileBlockUtil.getNodePath(node), isCopy);
					}
				}
			}
			for (final String fid : fidList) {
				if (fid == null || fid.length() <= 0) {
					return ERROR_PARAMETER;
				}
				final Folder folder = this.folderRepository.selectById(fid);
				if (folder == null) {
					return ERROR_PARAMETER;
				}
				if (folder.getFolderParent().equals(locationpath) && !isCopy) {
					continue;
				}
				if (!ConfigurationManager.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigurationManager.instance().authorized(account, AccountAuth.MOVE_FILES,
						folderUtil.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				if (!isCopy && !ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						folderUtil.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				if (!isCopy) {
					if (fid.equals(locationpath) || folderUtil.getParentList(locationpath).stream()
							.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
						return ERROR_PARAMETER;
					}
				}
				String originPath = folderUtil.getFolderPath(folder);
				String ip = ipAddrGetter.getIpAddr(request);
				if (folderRepository.selectByParentId(locationpath).stream()
						.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
					if (optMap.get(fid) == null) {
						return ERROR_PARAMETER;
					}
					switch (optMap.get(fid)) {
					case "cover":
						if (!ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								folderUtil.getAllFoldersId(locationpath))) {
							return NO_AUTHORIZED;
						}
						Folder f = folderRepository.selectByParentId(locationpath).stream()
								.filter((e) -> e.getFolderName().equals(folder.getFolderName())).findFirst().orElse(null);
						if (f == null) {
							break;
						}
						if (folderRepository.deleteById(f.getFolderId()) > 0) {
							if (isCopy) {
								Folder newFolder = folderUtil.copyFolderByNewNameToPath(folder, account, targetFolder, null);
								folderUtil.deleteAllChildFolder(f.getFolderId());
								if (newFolder != null) {
									this.logUtil.writeMoveFolderEvent(account, ip, originPath, folderUtil.getFolderPath(newFolder),
											isCopy);
									break;
								}
							} else {
								folderUtil.deleteAllChildFolder(f.getFolderId());
								folder.setFolderParent(locationpath);
								boolean needChangeChildsConstranint = false;
								if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
									folder.setFolderConstraint(targetFolder.getFolderConstraint());
									needChangeChildsConstranint = true;
								}
								if (this.folderRepository.update(folder) > 0) {
									if (needChangeChildsConstranint) {
										folderUtil.changeChildFolderConstraint(folder.getFolderId(),
												targetFolder.getFolderConstraint());
									}
									this.logUtil.writeMoveFolderEvent(account, ip, originPath, folderUtil.getFolderPath(folder),
											isCopy);
									break;
								}
							}
						}
						return "cannotMoveFiles";
					case "both":
						if (folderRepository.countByParentId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
							return FOLDERS_TOTAL_OUT_OF_LIMIT;
						}
						if (isCopy) {
							Folder newFolder = folderUtil.copyFolderByNewNameToPath(folder, account, targetFolder, FileNodeUtil
									.getNewFolderName(folder.getFolderName(), folderRepository.selectByParentId(locationpath)));
							if (newFolder == null) {
								return "cannotMoveFiles";
							}
							this.logUtil.writeMoveFolderEvent(account, ip, originPath, folderUtil.getFolderPath(newFolder), isCopy);
						} else {
							folder.setFolderParent(locationpath);
							folder.setFolderName(FileNodeUtil.getNewFolderName(folder.getFolderName(),
									folderRepository.selectByParentId(locationpath)));
							boolean needChangeChildsConstranint = false;
							if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
								folder.setFolderConstraint(targetFolder.getFolderConstraint());
								needChangeChildsConstranint = true;
							}
							if (this.folderRepository.update(folder) <= 0) {
								return "cannotMoveFiles";
							}
							if (needChangeChildsConstranint) {
								folderUtil.changeChildFolderConstraint(folder.getFolderId(),
										targetFolder.getFolderConstraint());
							}
							this.logUtil.writeMoveFolderEvent(account, ip, originPath, folderUtil.getFolderPath(folder), isCopy);
						}
						break;
					case "skip":
						break;
					default:
						return ERROR_PARAMETER;
					}
				} else {
					if (folderRepository.countByParentId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
						return FOLDERS_TOTAL_OUT_OF_LIMIT;
					}
					if (isCopy) {
						Folder newFolder = folderUtil.copyFolderByNewNameToPath(folder, account, targetFolder, null);
						if (newFolder == null) {
							return "cannotMoveFiles";
						}
						this.logUtil.writeMoveFolderEvent(account, ip, originPath, folderUtil.getFolderPath(newFolder), isCopy);
					} else {
						folder.setFolderParent(locationpath);
						boolean needChangeChildsConstranint = false;
						if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
							folder.setFolderConstraint(targetFolder.getFolderConstraint());
							needChangeChildsConstranint = true;
						}
						if (this.folderRepository.update(folder) <= 0) {
							return "cannotMoveFiles";
						}
						if (needChangeChildsConstranint) {
							folderUtil.changeChildFolderConstraint(folder.getFolderId(), targetFolder.getFolderConstraint());
						}
						this.logUtil.writeMoveFolderEvent(account, ip, originPath, folderUtil.getFolderPath(folder), isCopy);
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
		Folder targetFolder = folderRepository.selectById(locationpath);
		int needMovefilesCount = 0;
		int needMoveFoldersCount = 0;
		if (ConfigurationManager.instance().accessFolder(targetFolder, account) && ConfigurationManager.instance()
				.authorized(account, AccountAuth.MOVE_FILES, folderUtil.getAllFoldersId(locationpath))) {
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				if (fidList.size() > 0 && !ConfigurationManager.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						folderUtil.getAllFoldersId(locationpath))) {
					return NO_AUTHORIZED;
				}
				List<Node> repeNodes = new ArrayList<>();
				List<Folder> repeFolders = new ArrayList<>();
				for (final String fileId : idList) {
					if (fileId == null || fileId.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Node node = this.fileNodeRepository.selectById(fileId);
					if (node == null) {
						return ERROR_PARAMETER;
					}
					if (node.getFileParentFolder().equals(locationpath) && !isCopy) {
						continue;
					}
					if (!ConfigurationManager.instance().accessFolder(folderRepository.selectById(node.getFileParentFolder()), account)) {
						return NO_AUTHORIZED;
					}
					if (!ConfigurationManager.instance().authorized(account, AccountAuth.MOVE_FILES,
							folderUtil.getAllFoldersId(node.getFileParentFolder()))) {
						return NO_AUTHORIZED;
					}
					if (!isCopy && !ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							folderUtil.getAllFoldersId(node.getFileParentFolder()))) {
						return NO_AUTHORIZED;
					}
					if (fileNodeRepository.selectByParentFolderId(locationpath).stream()
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
					final Folder folder = this.folderRepository.selectById(folderId);
					if (folder == null) {
						return ERROR_PARAMETER;
					}
					if (folder.getFolderParent().equals(locationpath) && !isCopy) {
						continue;
					}
					if (!ConfigurationManager.instance().accessFolder(folder, account)) {
						return NO_AUTHORIZED;
					}
					if (!ConfigurationManager.instance().authorized(account, AccountAuth.MOVE_FILES,
							folderUtil.getAllFoldersId(folder.getFolderParent()))) {
						return NO_AUTHORIZED;
					}
					if (!isCopy && !ConfigurationManager.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							folderUtil.getAllFoldersId(folder.getFolderParent()))) {
						return NO_AUTHORIZED;
					}
					if (!isCopy) {
						if (folderId.equals(locationpath) || folderUtil.getParentList(locationpath).stream()
								.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
							return "CANT_MOVE_TO_INSIDE:" + folder.getFolderName();
						}
					}
					if (folderRepository.selectByParentId(locationpath).stream()
							.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
						repeFolders.add(folder);
					} else {
						needMoveFoldersCount++;
					}
				}
				long estimateFilesTotal = fileNodeRepository.countByParentFolderId(locationpath) + needMovefilesCount;
				if (estimateFilesTotal > FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER || estimateFilesTotal < 0) {
					return FILES_TOTAL_OUT_OF_LIMIT;
				}
				long estimateFoldersTotal = folderRepository.countByParentId(locationpath) + needMoveFoldersCount;
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
		Folder folder = folderRepository.selectById(folderId);
		if (folder == null) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.UPLOAD_FILES, folderUtil.getAllFoldersId(folderId))
				|| !ConfigurationManager.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						folderUtil.getAllFoldersId(folderId))
				|| !ConfigurationManager.instance().accessFolder(folder, account)) {
			cifr.setResult(NO_AUTHORIZED);
			return gson.toJson(cifr);
		}
		try {
			long mufs = Long.parseLong(maxUploadFileSize);
			long pMaxUploadSize = ConfigurationManager.instance().getUploadFileSize(account);
			if (pMaxUploadSize >= 0) {
				if (mufs > pMaxUploadSize) {
					cifr.setResult("fileOverSize");
					cifr.setMaxSize(SizeFormatUtil.formatFileSize(ConfigurationManager.instance().getUploadFileSize(account),
							"\u8bbe\u7f6e\u65e0\u6548\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458"));
					return gson.toJson(cifr);
				}
			}
		} catch (Exception e) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		final List<Folder> folders = folderRepository.selectByParentId(folderId);
		Folder testFolder = folders == null ? null
				: folders.stream().filter((n) -> n.getFolderName().equals(folderName)).findAny().orElse(null);
		if (testFolder != null) {
			if (ConfigurationManager.instance().accessFolder(testFolder, account) && ConfigurationManager.instance()
					.authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER, folderUtil.getAllFoldersId(folderId))) {
				cifr.setResult("repeatFolder_coverOrBoth");
			} else {
				cifr.setResult("repeatFolder_Both");
			}
		} else if (folderRepository.countByParentId(folderId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			cifr.setResult(FOLDERS_TOTAL_OUT_OF_LIMIT);
		} else {
			cifr.setResult("permitUpload");
		}
		return gson.toJson(cifr);
	}

	@Override
	@Transactional
	public String doImportFolder(HttpServletRequest request, MultipartFile file) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		String folderId = request.getParameter("folderId");
		final String originalFileName = request.getParameter("originalFileName");
		String folderConstraint = request.getParameter("folderConstraint");
		String newFolderName = request.getParameter("newFolderName");
		if (folderId == null || folderId.length() <= 0 || originalFileName == null || originalFileName.length() <= 0) {
			return UPLOADERROR;
		}
		Folder folder = folderRepository.selectById(folderId);
		if (folder == null) {
			return UPLOADERROR;
		}
		if (!ConfigurationManager.instance().authorized(account, AccountAuth.UPLOAD_FILES, folderUtil.getAllFoldersId(folderId))
				|| !ConfigurationManager.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						folderUtil.getAllFoldersId(folderId))
				|| !ConfigurationManager.instance().accessFolder(folder, account)) {
			return UPLOADERROR;
		}
		long mufs = ConfigurationManager.instance().getUploadFileSize(account);
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
				newFolder = folderUtil.createNewFolder(folderId, account, pName, folderConstraint);
			} catch (FoldersTotalOutOfLimitException e1) {
				return FOLDERS_TOTAL_OUT_OF_LIMIT;
			}
			if (newFolder == null) {
				Map<String, String> key = new HashMap<String, String>();
				key.put("parentId", folderId);
				key.put("folderName", pName);
				Folder target = folderRepository.selectByParentIdAndFolderName(key);
				if (target != null) {
					folderId = target.getFolderId();
				} else {
					return UPLOADERROR;
				}
			} else {
				if (!folderUtil.isValidFolder(newFolder)) {
					return UPLOADERROR;
				}
				folderId = newFolder.getFolderId();
			}
		}
		String fileName = getFileNameFormPath(originalFileName);
		if (fileName == null || fileName.length() <= 0
				|| !TextFormateUtil.instance().matcherFileName(fileName)) {
			return UPLOADERROR;
		}
		if (fileNodeRepository.countByParentFolderId(folderId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return FILES_TOTAL_OUT_OF_LIMIT;
		}
		final File block = this.fileBlockUtil.saveToFileBlocks(file);
		if (block == null) {
			return UPLOADERROR;
		}
		final String fsize = this.fileBlockUtil.getFileSize(file.getSize());
		int retryCount = 0;
		Node newNode = null;
		while (retryCount < 3) {
			List<Node> currentFiles = this.fileNodeRepository.selectByParentFolderId(folderId);
			if (currentFiles.stream().anyMatch((e) -> e.getFileName().equals(fileName))) {
				block.delete();
				return UPLOADERROR;
			}
			newNode = fileBlockUtil.insertNewNode(fileName, account, block.getName(), fsize, folderId);
			if (newNode != null) {
				break;
			}
			retryCount++;
		}
		if (newNode != null) {
			this.logUtil.writeUploadFileEvent(request, newNode, account);
			return UPLOADSUCCESS;
		} else {
			block.delete();
			return UPLOADERROR;
		}
	}

	private String[] getParentPath(String path) {
		if (path != null) {
			String normalizedPath = path.replace('\\', '/');
			String[] paths = normalizedPath.split("/");
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
			String normalizedPath = path.replace('\\', '/');
			String[] paths = normalizedPath.split("/");
			if (paths.length > 0) {
				return paths[paths.length - 1];
			}
		}
		return null;
	}

}
