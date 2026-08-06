package kohgylw.kiftd.newcore.service.impl;

import java.io.File;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.service.FileChainService;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.model.Property;
import kohgylw.kiftd.server.util.AESCipher;
import kohgylw.kiftd.server.util.ChainKeyMaster;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;


@Service
@Primary
public class FileChainServiceImpl implements FileChainService {

	private final NodeMapper nm;
	private final FileBlockUtil fbu;
	private final ContentTypeMap ctm;
	private final LogUtil lu;
	private final AESCipher cipher;
	private final ChainKeyMaster chainKeyMaster;
	private final PropertiesMapper pm;

	public FileChainServiceImpl(NodeMapper nm, FileBlockUtil fbu, ContentTypeMap ctm, LogUtil lu,
			AESCipher cipher, ChainKeyMaster chainKeyMaster, PropertiesMapper pm) {
		this.nm = nm;
		this.fbu = fbu;
		this.ctm = ctm;
		this.lu = lu;
		this.cipher = cipher;
		this.chainKeyMaster = chainKeyMaster;
		this.pm = pm;
	}

	@Override
	public void getResourceByChainKey(HttpServletRequest request, HttpServletResponse response) {
		int statusCode = 403;
		if (ConfigurationManager.instance().isOpenFileChain()) {
			final String ckey = request.getParameter("ckey");
			if (ckey != null) {
				Property keyProp = pm.selectByKey("chain_aes_key");
				if (keyProp != null) {
					try {
						String aesKey = chainKeyMaster.unwrap(keyProp.getPropertyValue());
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

}
