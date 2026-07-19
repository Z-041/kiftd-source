package kohgylw.kiftd.newcore.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.VideoTranscodeThread;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.service.ResourceService;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.NoticeUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.util.TxtCharsetGetter;
import kohgylw.kiftd.server.util.VariableSpeedBufferedOutputStream;
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

	private static final long RESOURCE_CACHE_MAX_AGE = 1800L;

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
						int status = sendResource(file, n.getFileName(), contentType, request, response);
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
		}
	}

	private int sendResource(File resource, String fname, String contentType, HttpServletRequest request,
			HttpServletResponse response) {
		int status = HttpServletResponse.SC_OK;
		try (RandomAccessFile randomFile = new RandomAccessFile(resource, "r")) {
			long contentLength = randomFile.length();
			final String lastModified = ServerTimeUtil.getLastModifiedFormBlock(resource);
			final String eTag = this.fileBlockUtil.getETag(resource);
			final String ifModifiedSince = request.getHeader("If-Modified-Since");
			final String ifNoneMatch = request.getHeader("If-None-Match");
			if (ifModifiedSince != null || ifNoneMatch != null) {
				if (ifNoneMatch != null) {
					if (ifNoneMatch.trim().equals(eTag)) {
						status = HttpServletResponse.SC_NOT_MODIFIED;
						response.setStatus(status);
						return status;
					}
				} else {
					if (ifModifiedSince.trim().equals(lastModified)) {
						status = HttpServletResponse.SC_NOT_MODIFIED;
						response.setStatus(status);
						return status;
					}
				}
			}
			String ifUnmodifiedSince = request.getHeader("If-Unmodified-Since");
			if (ifUnmodifiedSince != null && !(ifUnmodifiedSince.trim().equals(lastModified))) {
				status = HttpServletResponse.SC_PRECONDITION_FAILED;
				response.setStatus(status);
				return status;
			}
			String ifMatch = request.getHeader("If-Match");
			if (ifMatch != null && !(ifMatch.trim().equals(eTag))) {
				status = HttpServletResponse.SC_PRECONDITION_FAILED;
				response.setStatus(status);
				return status;
			}
			String range = request.getHeader("Range");
			long start = 0, end = 0;
			boolean hasExplicitEnd = false;
			if (range != null && range.startsWith("bytes=")) {
				try {
					String[] values = range.split("=")[1].split("-");
					start = Long.parseLong(values[0]);
					if (values.length > 1 && values[1].length() > 0) {
						end = Long.parseLong(values[1]);
						hasExplicitEnd = true;
					}
				} catch (NumberFormatException e) {
					start = 0;
					end = 0;
					hasExplicitEnd = false;
				}
			}
			long requestSize = 0;
			if (hasExplicitEnd && end > start) {
				requestSize = end - start + 1;
			} else {
				requestSize = Long.MAX_VALUE;
			}
			byte[] buffer = new byte[ConfigurationManager.instance().getBuffSize()];
			response.setContentType(contentType);
			response.setHeader("Accept-Ranges", "bytes");
			response.setHeader("ETag", this.fileBlockUtil.getETag(resource));
			response.setHeader("Last-Modified", ServerTimeUtil.getLastModifiedFormBlock(resource));
			response.setHeader("Cache-Control", "max-age=" + RESOURCE_CACHE_MAX_AGE);
			final String ifRange = request.getHeader("If-Range");
			if (range != null && range.startsWith("bytes=")
					&& (ifRange == null || ifRange.trim().equals(eTag) || ifRange.trim().equals(lastModified))) {
				status = HttpServletResponse.SC_PARTIAL_CONTENT;
				response.setStatus(status);
				long requestStart = 0, requestEnd = 0;
				boolean hasRequestEnd = false;
				String[] ranges = range.split("=");
				if (ranges.length > 1) {
					String[] rangeDatas = ranges[1].split("-");
					try {
						requestStart = Long.parseLong(rangeDatas[0]);
						if (rangeDatas.length > 1 && rangeDatas[1].length() > 0) {
							requestEnd = Long.parseLong(rangeDatas[1]);
							hasRequestEnd = true;
						}
					} catch (NumberFormatException e) {
						requestStart = 0;
						requestEnd = 0;
						hasRequestEnd = false;
					}
				}
				long length = 0;
				if (hasRequestEnd) {
					length = requestEnd - requestStart + 1;
					response.setHeader("Content-length", "" + length);
					response.setHeader("Content-Range",
							"bytes " + requestStart + "-" + requestEnd + "/" + contentLength);
				} else {
					length = contentLength - requestStart;
					response.setHeader("Content-length", "" + length);
					response.setHeader("Content-Range",
							"bytes " + requestStart + "-" + (contentLength - 1) + "/" + contentLength);
				}
			} else {
				response.setHeader("Content-length", contentLength + "");
			}
			ServletOutputStream rawOut = response.getOutputStream();
			long maxRate = ConfigurationManager.instance().getDownloadMaxRate(
					(String) request.getSession().getAttribute("ACCOUNT"));
			OutputStream out = new VariableSpeedBufferedOutputStream(rawOut, maxRate, request.getSession());
			try {
				long needSize = requestSize;
				randomFile.seek(start);
				while (needSize > 0) {
					int len = randomFile.read(buffer);
					if (len <= 0) {
						break;
					}
					if (needSize < buffer.length) {
						out.write(buffer, 0, (int) needSize);
					} else {
						out.write(buffer, 0, len);
						if (len < buffer.length) {
							break;
						}
					}
					needSize -= buffer.length;
				}
			} finally {
				out.close();
			}
			return status;
		} catch (Exception e) {
			status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			return status;
		}
	}

	@Override
	public String getVideoTranscodeStatus(HttpServletRequest request) {
		if (kiftdFFMPEGLocator.isEnableFFmpeg()) {
			String fId = request.getParameter("fileId");
			if (fId != null) {
				try {
					return videoTranscodeUtil.getTranscodeProcess(fId);
				} catch (Exception e) {
					Printer.instance.print("错误：视频转码功能出现意外错误。详细信息：" + e.getMessage());
					logUtil.writeException(e);
				}
			}
		}
		return "ERROR";
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
								&& ifModifiedSince.trim().equals(ServerTimeUtil.getLastModifiedFormBlock(file))) {
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
							response.setHeader("Last-Modified", ServerTimeUtil.getLastModifiedFormBlock(file));
							response.setHeader("Cache-Control", "max-age=" + RESOURCE_CACHE_MAX_AGE);
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
		} catch (Exception e1) {
			this.logUtil.writeException(e1);
		}
	}

	@Override
	public void getNoticeContext(HttpServletRequest request, HttpServletResponse response) {
		File noticeHTML = new File(ConfigurationManager.instance().getTemporaryfilePath(), NoticeUtil.NOTICE_OUTPUT_NAME);
		String contentType = "text/html";
		if (noticeHTML.isFile() && noticeHTML.canRead()) {
			sendResource(noticeHTML, NoticeUtil.NOTICE_FILE_NAME, contentType, request, response);
		} else {
			try {
				response.setContentType(contentType);
				response.setCharacterEncoding("UTF-8");
				PrintWriter writer = response.getWriter();
				writer.write("<p class=\"lead\">暂无新公告。</p>");
				writer.flush();
				writer.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public String getNoticeMD5() {
		return noticeUtil.getMd5();
	}

}
