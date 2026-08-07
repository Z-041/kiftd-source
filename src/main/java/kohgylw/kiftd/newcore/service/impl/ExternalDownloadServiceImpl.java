package kohgylw.kiftd.newcore.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.domain.AjaxProtocol;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.newcore.service.ExternalDownloadService;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;



@Service
@Primary
public class ExternalDownloadServiceImpl implements ExternalDownloadService {

	private static final int MAX_DOWNLOAD_KEYS = 1000;
	private static final long DOWNLOAD_KEY_EXPIRE_MS = TimeUnit.HOURS.toMillis(1);

	private static final Map<String, DownloadKeyEntry> downloadKeyMap = new LinkedHashMap<String, DownloadKeyEntry>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, DownloadKeyEntry> eldest) {
			return size() > MAX_DOWNLOAD_KEYS || (eldest != null
					&& System.currentTimeMillis() - eldest.getValue().createTime > DOWNLOAD_KEY_EXPIRE_MS);
		}
	};

	private static class DownloadKeyEntry {
		final String fileId;
		final long createTime;

		DownloadKeyEntry(String fileId) {
			this.fileId = fileId;
			this.createTime = System.currentTimeMillis();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof DownloadKeyEntry))
				return false;
			return fileId.equals(((DownloadKeyEntry) o).fileId);
		}

		@Override
		public int hashCode() {
			return fileId.hashCode();
		}
	}
	private static final String CONTENT_TYPE = "application/octet-stream";

	private final FileNodeRepository fileNodeRepository;
	private final FolderRepository folderRepository;
	private final LogUtil logUtil;
	private final FileBlockUtil fileBlockUtil;
	private final FolderUtil folderUtil;

	public ExternalDownloadServiceImpl(FileNodeRepository fileNodeRepository, FolderRepository folderRepository,
			LogUtil logUtil, FileBlockUtil fileBlockUtil, FolderUtil folderUtil) {
		this.fileNodeRepository = fileNodeRepository;
		this.folderRepository = folderRepository;
		this.logUtil = logUtil;
		this.fileBlockUtil = fileBlockUtil;
		this.folderUtil = folderUtil;
	}

	@Override
	public String getDownloadKey(HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String fileId = request.getParameter("fId");
		if (fileId != null) {
			final Node f = this.fileNodeRepository.selectById(fileId);
			if (f != null) {
				if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						folderUtil.getAllFoldersId(f.getFileParentFolder()))
						&& ConfigurationManager.instance().accessFolder(folderRepository.selectById(f.getFileParentFolder()), account)) {
					synchronized (downloadKeyMap) {
						this.logUtil.writeShareFileURLEvent(request, f);
						cleanExpiredKeys();
						if (downloadKeyMap.containsValue(new DownloadKeyEntry(f.getFileId()))) {
							Entry<String, DownloadKeyEntry> k = downloadKeyMap.entrySet().stream()
									.filter((e) -> e.getValue().fileId.equals(f.getFileId())).findFirst().orElse(null);
							if (k != null) {
								return k.getKey();
							}
						}
						String dKey = UUID.randomUUID().toString();
						downloadKeyMap.put(dKey, new DownloadKeyEntry(f.getFileId()));
						return dKey;
					}
				}
			}
		}
		return AjaxProtocol.ERROR;
	}

	@Override
	public void downloadFileByKey(HttpServletRequest request, HttpServletResponse response) {
		final String dkey = request.getParameter("dkey");
		if (dkey != null) {
			String fId = null;
			synchronized (downloadKeyMap) {
				// 密钥在 TTL 内可复用而非一次性消费：视频播放器等客户端的 Range 分片/续传请求
				// 会复用同一 URL 多次访问，若首请求即移除密钥则后续分片全部 404
				DownloadKeyEntry entry = downloadKeyMap.get(dkey);
				if (entry != null) {
					// cleanExpiredKeys 仅在生成新 key 时惰性触发，使用侧必须兜底校验过期时间
					if (System.currentTimeMillis() - entry.createTime <= DOWNLOAD_KEY_EXPIRE_MS) {
						fId = entry.fileId;
					}
				}
			}
			if (fId != null) {
				Node f = this.fileNodeRepository.selectById(fId);
				if (f != null) {
					File target = this.fileBlockUtil.getFileFromBlocks(f);
					if (target != null && target.isFile()) {
						String range = request.getHeader("Range");
						int status = RangeFileStreamWriter.writeRangeFileStream(request, response, target, f.getFileName(), CONTENT_TYPE,
								ConfigurationManager.instance().getDownloadMaxRate(null), fileBlockUtil.getETag(target), true);
						if (status == HttpServletResponse.SC_OK || (range != null && range.startsWith("bytes=0-"))) {
							this.logUtil.writeDownloadFileByKeyEvent(request, f);
						}
						return;
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

	private static void cleanExpiredKeys() {
		long now = System.currentTimeMillis();
		downloadKeyMap.entrySet().removeIf(
				e -> now - e.getValue().createTime > DOWNLOAD_KEY_EXPIRE_MS);
	}

}
