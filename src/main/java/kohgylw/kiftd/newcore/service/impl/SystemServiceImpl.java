package kohgylw.kiftd.newcore.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.newcore.infrastructure.crypto.CryptoService;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.newcore.repository.PropertiesRepository;
import kohgylw.kiftd.newcore.service.SystemService;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.model.Propertie;
import kohgylw.kiftd.server.util.ChainKeyMaster;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 *
 * <h2>系统服务实现类</h2>
 * <p>
 * 该类实现了 SystemService 接口中定义的系统相关业务逻辑，
 * 包括获取服务器操作系统名称和根据文件 ID 获取资源链密钥等功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.newcore.service.SystemService
 */
@Service
@Primary
public class SystemServiceImpl implements SystemService {

	private final FileNodeRepository fileNodeRepository;
	private final FolderRepository folderRepository;
	private final PropertiesRepository propertiesRepository;
	private final CryptoService cryptoService;
	private final ChainKeyMaster chainKeyMaster;
	private final FolderUtil folderUtil;
	private final LogUtil logUtil;

	public SystemServiceImpl(FileNodeRepository fileNodeRepository, FolderRepository folderRepository,
			PropertiesRepository propertiesRepository, CryptoService cryptoService, ChainKeyMaster chainKeyMaster,
			FolderUtil folderUtil, LogUtil logUtil) {
		this.fileNodeRepository = fileNodeRepository;
		this.folderRepository = folderRepository;
		this.propertiesRepository = propertiesRepository;
		this.cryptoService = cryptoService;
		this.chainKeyMaster = chainKeyMaster;
		this.folderUtil = folderUtil;
		this.logUtil = logUtil;
	}

	@Override
	public String getOSName() {
		return System.getProperty("os.name");
	}

	@Override
	public String getFileChainKey(HttpServletRequest request) {
		if (ConfigurationManager.instance().isOpenFileChain()) {
			String fid = request.getParameter("fid");
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (fid != null) {
				final Node f = this.fileNodeRepository.selectById(fid);
				if (f != null) {
					if (ConfigurationManager.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
							folderUtil.getAllFoldersId(f.getFileParentFolder()))) {
						Folder folder = folderRepository.selectById(f.getFileParentFolder());
						if (folder != null && ConfigurationManager.instance().accessFolder(folder, account)) {
							try {
								Propertie keyProp = propertiesRepository.selectByKey("chain_aes_key");
								if (keyProp == null) {
									String aesKey = cryptoService.generateRandomAesKey();
									Propertie chainAESKey = new Propertie();
									chainAESKey.setPropertieKey("chain_aes_key");
									chainAESKey.setPropertieValue(chainKeyMaster.wrap(aesKey));
									if (propertiesRepository.insert(chainAESKey) > 0) {
										return cryptoService.encrypt(aesKey, fid);
									}
								} else {
									String aesKey = chainKeyMaster.unwrap(keyProp.getPropertieValue());
									if (!chainKeyMaster.isWrapped(keyProp.getPropertieValue())) {
										keyProp.setPropertieValue(chainKeyMaster.wrap(aesKey));
										propertiesRepository.update(keyProp);
									}
									return cryptoService.encrypt(aesKey, fid);
								}
							} catch (Exception e) {
								logUtil.writeException(e);
							}
						}
					}
				}
			}
		}
		return null;
	}
}
