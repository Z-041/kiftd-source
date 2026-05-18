package kohgylw.kiftd.server.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.service.ExternalDownloadService;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;

/**
 *
 * <h2>外部下载服务实现类</h2>
 * <p>
 * 该类实现了 ExternalDownloadService 接口中定义的外部下载相关业务逻辑，
 * 包括生成下载凭证（download key）以及通过凭证进行文件下载等功能。
 * 外部下载机制允许用户生成一个临时的下载链接分享给其他用户，
 * 无需登录即可通过该凭证下载指定的文件资源。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.server.service.ExternalDownloadService
 */
@Service
public class ExternalDownloadServiceImpl extends RangeFileStreamWriter implements ExternalDownloadService {

	private static Map<String, String> downloadKeyMap = new HashMap<>();
	private static final String CONTENT_TYPE = "application/octet-stream";

	@Resource
	private NodeMapper nm;
	@Resource
	private LogUtil lu;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private FolderUtil fu;
	@Resource
	private FolderMapper fm;

	@Override
	public String getDownloadKey(HttpServletRequest request) {
		// 首先进行权限检查
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 找到要下载的文件节点
		final String fileId = request.getParameter("fId");
		if (fileId != null) {
			final Node f = this.nm.queryById(fileId);
			if (f != null) {
				// 权限检查
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(f.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(fm.queryById(f.getFileParentFolder()), account)) {
					// 获取凭证
					synchronized (downloadKeyMap) {
						// 查找该资源是否已经生成了一个凭证，如有，则直接使用，否则，新生成一个加入到凭证表。
						this.lu.writeShareFileURLEvent(request, f);
						if (downloadKeyMap.containsValue(f.getFileId())) {
							Entry<String, String> k = downloadKeyMap.entrySet().stream()
									.filter((e) -> e.getValue().equals(f.getFileId())).findFirst().get();
							return k.getKey();
						} else {
							String dKey = UUID.randomUUID().toString();
							downloadKeyMap.put(dKey, f.getFileId());
							return dKey;
						}
					}
				}
			}
		}
		return "ERROR";
	}

	@Override
	public void downloadFileByKey(HttpServletRequest request, HttpServletResponse response) {
		final String dkey = request.getParameter("dkey");
		// 权限凭证有效性并确认其对应的资源
		if (dkey != null) {
			// 找到要下载的文件节点
			String fId = null;
			synchronized (downloadKeyMap) {
				fId = downloadKeyMap.get(dkey);
			}
			if (fId != null) {
				Node f = this.nm.queryById(fId);
				if (f != null) {
					File target = this.fbu.getFileFromBlocks(f);
					if (target != null && target.isFile()) {
						String range = request.getHeader("Range");
						int status = writeRangeFileStream(request, response, target, f.getFileName(), CONTENT_TYPE,
								ConfigureReader.instance().getDownloadMaxRate(null), fbu.getETag(target), true);
						if (status == HttpServletResponse.SC_OK || (range != null && range.startsWith("bytes=0-"))) {
							this.lu.writeDownloadFileByKeyEvent(request, f);
						}
						return;
					}
				}
			}
		}
		try {
			//  处理无法下载的资源
			response.sendError(404);
		} catch (IOException e) {

		}
	}

}
