package kohgylw.kiftd.newcore.service.impl;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.service.FileChainService;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.model.Propertie;
import kohgylw.kiftd.server.util.AESCipher;
import kohgylw.kiftd.server.util.ChainKeyMaster;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;

@Service
@Primary
public class FileChainServiceImpl implements FileChainService {

	private final NodeMapper nm;
	private final FolderMapper flm;
	private final FileBlockUtil fbu;
	private final ContentTypeMap ctm;
	private final LogUtil lu;
	private final AESCipher cipher;
	private final ChainKeyMaster chainKeyMaster;
	private final PropertiesMapper pm;
	private final FolderUtil fu;

	public FileChainServiceImpl(NodeMapper nm, FolderMapper flm, FileBlockUtil fbu, ContentTypeMap ctm, LogUtil lu,
			AESCipher cipher, ChainKeyMaster chainKeyMaster, PropertiesMapper pm, FolderUtil fu) {
		this.nm = nm;
		this.flm = flm;
		this.fbu = fbu;
		this.ctm = ctm;
		this.lu = lu;
		this.cipher = cipher;
		this.chainKeyMaster = chainKeyMaster;
		this.pm = pm;
		this.fu = fu;
	}

	@Override
	public void getResourceByChainKey(HttpServletRequest request, HttpServletResponse response) {
		int statusCode = 403;
		if (ConfigurationManager.instance().isOpenFileChain()) {
			final String ckey = request.getParameter("ckey");
			if (ckey != null) {
				Propertie keyProp = pm.selectByKey("chain_aes_key");
				if (keyProp != null) {
					try {
						String aesKey = chainKeyMaster.unwrap(keyProp.getPropertieValue());
						String fid = cipher.decrypt(aesKey, ckey);
						Node f = this.nm.selectById(fid);
						if (f != null) {
							File target = this.fbu.getFileFromBlocks(f);
							if (target != null && target.isFile()) {
								String fileName = f.getFileName();
								String suffix = "";
								if (fileName.indexOf(".") >= 0) {
									suffix = fileName.substring(fileName.lastIndexOf(".")).trim().toLowerCase();
								}
								String range = request.getHeader("Range");
								int status = RangeFileStreamWriter.writeRangeFileStream(request, response, target,
										f.getFileName(), ctm.getContentType(suffix),
										ConfigurationManager.instance().getDownloadMaxRate(null), fbu.getETag(target),
										false);
								if (status == HttpServletResponse.SC_OK
										|| (range != null && range.startsWith("bytes=0-"))) {
									this.lu.writeChainEvent(request, f);
								}
								return;
							}
						}
						statusCode = 404;
					} catch (Exception e) {
						lu.writeException(e);
						statusCode = 500;
					}
				} else {
					statusCode = 404;
				}
			}
		}
		try {
			response.sendError(statusCode);
		} catch (IOException e) {
			this.lu.writeException(e);
		}
	}

	@Override
	public String getChainKeyByFid(HttpServletRequest request) {
		if (ConfigurationManager.instance().isOpenFileChain()) {
			String fid = request.getParameter("fid");
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (fid != null) {
				final Node f = this.nm.selectById(fid);
				if (f != null) {
					if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
							fu.getAllFoldersId(f.getFileParentFolder()))) {
						Folder folder = flm.selectById(f.getFileParentFolder());
						if (folder != null && ConfigurationManager.instance().accessFolder(folder, account)) {
							try {
								Propertie keyProp = pm.selectByKey("chain_aes_key");
								if (keyProp == null) {
									String aesKey = cipher.generateRandomKey();
									Propertie chainAESKey = new Propertie();
									chainAESKey.setPropertieKey("chain_aes_key");
									chainAESKey.setPropertieValue(chainKeyMaster.wrap(aesKey));
									if (pm.insert(chainAESKey) > 0) {
										return cipher.encrypt(aesKey, fid);
									}
								} else {
									String aesKey = chainKeyMaster.unwrap(keyProp.getPropertieValue());
									if (!chainKeyMaster.isWrapped(keyProp.getPropertieValue())) {
										keyProp.setPropertieValue(chainKeyMaster.wrap(aesKey));
										pm.update(keyProp);
									}
									return cipher.encrypt(aesKey, fid);
								}
							} catch (Exception e) {
								lu.writeException(e);
							}
						}
					}
				}
			}
		}
		return "ERROR";
	}

}
