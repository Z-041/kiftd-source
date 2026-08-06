package kohgylw.kiftd.newcore.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.domain.AjaxProtocol;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.newcore.service.MediaService;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.PictureInfo;
import kohgylw.kiftd.server.pojo.PictureViewList;
import kohgylw.kiftd.server.pojo.VideoInfo;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;
import kohgylw.kiftd.server.util.LogUtil;
import net.coobird.thumbnailator.Thumbnails;
import ws.schild.jave.MultimediaObject;


/**
 *
 * <h2>媒体服务实现类</h2>
 * <p>
 * 该类实现了 MediaService 接口中定义的媒体相关业务逻辑，包括图片预览、图片压缩以及视频播放等功能。
 * 整合了原有的 ShowPictureService 和 PlayVideoService 的全部业务逻辑。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.newcore.service.MediaService
 */
@Service
@Primary
public class MediaServiceImpl implements MediaService {

	private final FileNodeRepository fileNodeRepository;
	private final FolderRepository folderRepository;
	private final Gson gson;
	private final FileBlockUtil fileBlockUtil;
	private final FolderUtil folderUtil;
	private final LogUtil logUtil;
	private final KiftdFFMPEGLocator kiftdFFMPEGLocator;

	public MediaServiceImpl(FileNodeRepository fileNodeRepository, FolderRepository folderRepository, Gson gson,
			FileBlockUtil fileBlockUtil, FolderUtil folderUtil, LogUtil logUtil,
			KiftdFFMPEGLocator kiftdFFMPEGLocator) {
		this.fileNodeRepository = fileNodeRepository;
		this.folderRepository = folderRepository;
		this.gson = gson;
		this.fileBlockUtil = fileBlockUtil;
		this.folderUtil = folderUtil;
		this.logUtil = logUtil;
		this.kiftdFFMPEGLocator = kiftdFFMPEGLocator;
	}

	/**
	 * 
	 * <h2>获取所有同级目录下的图片并封装为PictureViewList对象</h2>
	 * <p>
	 * 该方法用于根据请求获取预览图片列表并进行封装，对于过大图片会进行压缩。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *                HttpServletRequest 请求对象，需包含fileId字段（需预览的图片ID）。
	 * @return PictureViewList 预览列表封装对象，详见其注释。
	 * @see kohgylw.kiftd.server.pojo.PictureViewList
	 */
	private static final Set<String> PICTURE_SUFFIXES = Set.of("jpg", "jpeg", "bmp", "png", "gif");
	private static final Set<String> TRANSCODE_VIDEO_SUFFIXES = Set.of("mkv", "mov", "webm", "avi", "wmv", "flv");
	private static final Set<String> MP4_VIDEO_SUFFIXES = Set.of("mp4");

	private PictureViewList foundPictures(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		if (fileId != null && fileId.length() > 0) {
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			Node p = fileNodeRepository.selectById(fileId);
			if (p != null) {
				if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(p.getFileParentFolder()))
						&& ConfigurationManager.instance().accessFolder(folderRepository.selectById(p.getFileParentFolder()), account)) {
					final List<Node> nodes = this.fileNodeRepository.selectBySomeFolder(fileId);
					final List<PictureInfo> pictureViewList = new ArrayList<>();
					int index = 0;
					for (final Node n : nodes) {
						final String fileName = n.getFileName();
						final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
						if (PICTURE_SUFFIXES.contains(suffix)) {
							PictureInfo pi = new PictureInfo();
							pi.setFileName(fileName);
							long pSize = Long.parseLong(n.getFileSize());
							File block = fileBlockUtil.getFileFromBlocks(n);
							if (block == null) {
								// 文件块缺失（数据库记录存在但磁盘文件已被清理）时跳过，避免 NPE
								continue;
							}
							long lastModified = block.lastModified();
							if (pSize > 1 && !suffix.equals("gif")) {
								pi.setUrl("homeController/showCondensedPicture.do?fileId=" + n.getFileId()
										+ "&lastmodified=" + lastModified);
							} else {
								pi.setUrl("resourceController/getResource/" + n.getFileId() + "?lastmodified="
										+ lastModified);
							}
							pictureViewList.add(pi);
							if (n.getFileId().equals(fileId)) {
								index = pictureViewList.size() - 1;
							}
						}
					}
					final PictureViewList pvl = new PictureViewList();
					pvl.setIndex(index);
					pvl.setPictureViewList(pictureViewList);
					return pvl;
				}
			}
		}
		return null;
	}

	@Override
	public String getPreviewPictureJson(final HttpServletRequest request) {
		final PictureViewList pvl = this.foundPictures(request);
		if (pvl != null) {
			return gson.toJson((Object) pvl);
		}
		return AjaxProtocol.ERROR;
	}

	@Override
	public void getCondensedPicture(final HttpServletRequest request, final HttpServletResponse response) {

		String fileId = request.getParameter("fileId");
		String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fileId != null) {
			Node node = fileNodeRepository.selectById(fileId);
			if (node != null) {
				if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(node.getFileParentFolder()))
						&& ConfigurationManager.instance().accessFolder(folderRepository.selectById(node.getFileParentFolder()),
								account)) {
					File pBlock = fileBlockUtil.getFileFromBlocks(node);
					if (pBlock != null && pBlock.exists()) {
						try {
							long pSize = Long.parseLong(node.getFileSize());
							String format = "JPG";
							int size;
							if (pSize < 3) {
								size = 1080;
							} else if (pSize < 5) {
								size = 1440;
							} else {
								size = 1680;
							}
							Thumbnails.of(pBlock).size(size, size).outputFormat(format)
									.toOutputStream(response.getOutputStream());
						} catch (IOException e) {
							try {
								Files.copy(pBlock.toPath(), response.getOutputStream());
							} catch (IOException e1) {
								logUtil.writeException(e1);
							}
						}
						return;
					}
				} else {
					try {
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
					} catch (IOException e) {
						logUtil.writeException(e);
					}
					return;
				}
			}
		}
		try {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (IOException e) {
			logUtil.writeException(e);
		}
	}

	private VideoInfo foundVideo(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		if (fileId != null && fileId.length() > 0) {
			final Node f = this.fileNodeRepository.selectById(fileId);
			if (f != null) {
				final VideoInfo vi = new VideoInfo(f);
				final String account = (String) request.getSession().getAttribute("ACCOUNT");
				if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(f.getFileParentFolder()))
						&& ConfigurationManager.instance().accessFolder(folderRepository.selectById(f.getFileParentFolder()), account)) {
					final String fileName = f.getFileName();
					final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
					if (MP4_VIDEO_SUFFIXES.contains(suffix)) {
						if (kiftdFFMPEGLocator.isEnableFFmpeg()) {
							File target = fileBlockUtil.getFileFromBlocks(f);
							if (target == null || !target.isFile()) {
								return null;
							}
							MultimediaObject mo = new MultimediaObject(target, kiftdFFMPEGLocator);
							try {
								if (mo.getInfo().getVideo().getDecoder().indexOf("h264") >= 0) {
									vi.setNeedEncode("N");
									return vi;
								}
							} catch (Exception e) {
								Printer.instance
										.print("错误：视频文件“" + f.getFileName() + "”在解析时出现意外错误。详细信息：" + e.getMessage());
								logUtil.writeException(e);
							}
							vi.setNeedEncode("Y");
						} else {
							vi.setNeedEncode("N");
						}
						return vi;
					} else if (TRANSCODE_VIDEO_SUFFIXES.contains(suffix)) {
						vi.setNeedEncode(kiftdFFMPEGLocator.isEnableFFmpeg() ? "Y" : "N");
						return vi;
					}
				}
			}
		}
		return null;
	}

	@Override
	public String getPlayVideoJson(final HttpServletRequest request) {
		final VideoInfo v = this.foundVideo(request);
		if (v != null) {
			return gson.toJson((Object) v);
		}
		return AjaxProtocol.ERROR;
	}
}
