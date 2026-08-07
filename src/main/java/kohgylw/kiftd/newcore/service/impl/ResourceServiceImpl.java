package kohgylw.kiftd.newcore.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.domain.AjaxProtocol;
import kohgylw.kiftd.newcore.service.ResourceService;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.VideoTranscodeThread;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.NoticeUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.util.TxtCharsetGetter;
import kohgylw.kiftd.server.util.VideoTranscodeUtil;


@Service
@Primary
public class ResourceServiceImpl implements ResourceService {

	private final NodeMapper nodeMapper;
	private final FileBlockUtil fileBlockUtil;
	private final LogUtil logUtil;
	private final VideoTranscodeUtil videoTranscodeUtil;
	private final FolderUtil folderUtil;
	private final FolderMapper folderMapper;
	private final NoticeUtil noticeUtil;
	private final TxtCharsetGetter txtCharsetGetter;
	private final ContentTypeMap contentTypeMap;
	private final KiftdFFMPEGLocator kiftdFFMPEGLocator;
	private final IpAddrGetter ipAddrGetter;

	public ResourceServiceImpl(NodeMapper nodeMapper, FileBlockUtil fileBlockUtil, LogUtil logUtil,
			VideoTranscodeUtil videoTranscodeUtil, FolderUtil folderUtil, FolderMapper folderMapper,
			NoticeUtil noticeUtil, TxtCharsetGetter txtCharsetGetter, ContentTypeMap contentTypeMap,
			KiftdFFMPEGLocator kiftdFFMPEGLocator, IpAddrGetter ipAddrGetter) {
		this.nodeMapper = nodeMapper;
		this.fileBlockUtil = fileBlockUtil;
		this.logUtil = logUtil;
		this.videoTranscodeUtil = videoTranscodeUtil;
		this.folderUtil = folderUtil;
		this.folderMapper = folderMapper;
		this.noticeUtil = noticeUtil;
		this.txtCharsetGetter = txtCharsetGetter;
		this.contentTypeMap = contentTypeMap;
		this.kiftdFFMPEGLocator = kiftdFFMPEGLocator;
		this.ipAddrGetter = ipAddrGetter;
	}

	@Override
	public void getResource(String fid, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fid != null) {
			Node n = nodeMapper.selectById(fid);
			if (n != null) {
				if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigurationManager.instance().accessFolder(folderMapper.selectById(n.getFileParentFolder()), account)) {
					File file = fileBlockUtil.getFileFromBlocks(n);
					if (file != null && file.isFile()) {
						String fileName = n.getFileName();
						String suffix = "";
						int lastDotIndex = fileName.lastIndexOf(".");
						if (lastDotIndex >= 0) {
							suffix = fileName.substring(lastDotIndex).trim().toLowerCase();
						}
						String contentType = contentTypeMap.getContentType(suffix);
						switch (suffix) {
						case ".mp4":
						case ".webm":
						case ".mov":
						case ".avi":
						case ".wmv":
						case ".mkv":
						case ".flv":
							if (kiftdFFMPEGLocator.isEnableFFmpeg()) {
								contentType = "video/mp4";
								synchronized (VideoTranscodeUtil.videoTranscodeThreads) {
									VideoTranscodeThread vtt = VideoTranscodeUtil.videoTranscodeThreads.get(fid);
									if (vtt != null) {
										File f = new File(ConfigurationManager.instance().getTemporaryfilePath(),
												vtt.getOutputFileName());
										if (f.isFile() && vtt.getProgress().equals("FIN")) {
											file = f;
										} else {
											try {
												response.sendError(500);
											} catch (IOException e) {
												this.logUtil.writeException(e);
											}
											return;
										}
									}
								}
							}
							break;
						default:
							break;
						}
						String ip = ipAddrGetter.getIpAddr(request);
						String range = request.getHeader("Range");
						int status = RangeFileStreamWriter.writeRangeFileStream(request, response, file, fileName,
								contentType, ConfigurationManager.instance().getDownloadMaxRate(account),
								fileBlockUtil.getETag(file), false);
						if (status == HttpServletResponse.SC_OK || (range != null && range.startsWith("bytes=0-"))) {
							this.logUtil.writeDownloadFileEvent(account, ip, n);
						}
						return;
					}
				} else {
					try {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					} catch (IOException e) {
						this.logUtil.writeException(e);
					}
					return;
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
	public String getVideoTranscodeStatus(HttpServletRequest request) {
		if (kiftdFFMPEGLocator.isEnableFFmpeg()) {
			// 转码会占用服务器CPU与磁盘资源，须校验登录态及下载权限，防止未授权用户反复触发转码
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			String fId = request.getParameter("fileId");
			if (account != null && fId != null) {
				Node n = nodeMapper.selectById(fId);
				if (n != null && ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigurationManager.instance().accessFolder(folderMapper.selectById(n.getFileParentFolder()), account)) {
					try {
						return videoTranscodeUtil.getTranscodeProcess(fId);
					} catch (Exception e) {
						Printer.instance.print("错误：视频转码功能出现意外错误。详细信息：" + e.getMessage());
						logUtil.writeException(e);
					}
				}
			}
		}
		return AjaxProtocol.ERROR;
	}

	@Override
	public void getLRContextByUTF8(String fileId, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String ip = ipAddrGetter.getIpAddr(request);
		if (fileId != null) {
			Node n = nodeMapper.selectById(fileId);
			if (n != null) {
				if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigurationManager.instance().accessFolder(folderMapper.selectById(n.getFileParentFolder()), account)) {
					File file = fileBlockUtil.getFileFromBlocks(n);
					if (file != null && file.isFile()) {
						String ifModifiedSince = request.getHeader("If-Modified-Since");
						if (ifModifiedSince != null
								&& ifModifiedSince.trim().equals(ServerTimeUtil.getLastModifiedFromBlock(file))) {
							response.setStatus(304);
							return;
						}
						String ifNoneMatch = request.getHeader("If-None-Match");
						if (ifNoneMatch != null && ifNoneMatch.trim().equals(this.fileBlockUtil.getETag(file))) {
							response.setStatus(304);
							return;
						}
						String fileName = n.getFileName();
						String suffix = "";
						int lastDotIndex = fileName.lastIndexOf(".");
						if (lastDotIndex >= 0) {
							suffix = fileName.substring(lastDotIndex).trim().toLowerCase();
						}
						if (".lrc".equals(suffix)) {
							String contentType = "text/plain";
							response.setContentType(contentType);
							response.setCharacterEncoding("UTF-8");
							response.setHeader("ETag", this.fileBlockUtil.getETag(file));
							response.setHeader("Last-Modified", ServerTimeUtil.getLastModifiedFromBlock(file));
							response.setHeader("Cache-Control", "max-age=" + RangeFileStreamWriter.DOWNLOAD_CACHE_MAX_AGE);
							try {
								String inputFileEncode;
								try (FileInputStream fis = new FileInputStream(file)) {
									inputFileEncode = txtCharsetGetter.getTxtCharset(fis);
								}
								try (BufferedReader bufferedReader = new BufferedReader(
										new InputStreamReader(new FileInputStream(file), inputFileEncode));
										BufferedWriter bufferedWriter = new BufferedWriter(
												new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
									String line;
									while ((line = bufferedReader.readLine()) != null) {
										bufferedWriter.write(line);
										bufferedWriter.newLine();
									}
								}
								this.logUtil.writeDownloadFileEvent(account, ip, n);
								return;
							} catch (IOException e) {
								logUtil.writeException(e);
							} catch (Exception e) {
								Printer.instance.print(e.getMessage());
							}
						}
					}
				}
			}
		}
		try {
			response.sendError(500);
		} catch (IOException e1) {
			this.logUtil.writeException(e1);
		}
	}

	@Override
	public void getNoticeContext(HttpServletRequest request, HttpServletResponse response) {
		File noticeHTML = new File(ConfigurationManager.instance().getTemporaryfilePath(), NoticeUtil.NOTICE_OUTPUT_NAME);
		String contentType = "text/html";
		if (noticeHTML.isFile() && noticeHTML.canRead()) {
			RangeFileStreamWriter.writeRangeFileStream(request, response, noticeHTML, NoticeUtil.NOTICE_OUTPUT_NAME,
					contentType,
					ConfigurationManager.instance()
							.getDownloadMaxRate((String) request.getSession().getAttribute("ACCOUNT")),
					fileBlockUtil.getETag(noticeHTML), false);
		} else {
			try {
				response.setContentType(contentType);
				response.setCharacterEncoding("UTF-8");
				PrintWriter writer = response.getWriter();
				writer.write("<p class=\"lead\">暂无新公告。</p>");
				writer.flush();
				writer.close();
			} catch (IOException e) {
				this.logUtil.writeException(e);
			}
		}
	}

	@Override
	public String getNoticeMD5() {
		return noticeUtil.getMd5();
	}

}
