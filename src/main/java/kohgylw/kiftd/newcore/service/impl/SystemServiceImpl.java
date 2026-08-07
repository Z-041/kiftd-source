package kohgylw.kiftd.newcore.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.infrastructure.crypto.CryptoService;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.newcore.repository.PropertiesRepository;
import kohgylw.kiftd.newcore.service.SystemService;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.model.Property;
import kohgylw.kiftd.server.util.ChainKeyMaster;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;


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

	private static final String CHAIN_AES_KEY = "chain_aes_key";
	// 链密钥初始化/迁移的专用锁：仅序列化首次初始化与旧格式迁移的读-改-写，
	// 常见路径（已初始化且为包装格式）无锁读取，避免与整个服务实例耦合
	private static final Object CHAIN_KEY_LOCK = new Object();

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
								// 常见路径：密钥已初始化且为包装格式，只读无需加锁
								Property keyProp = propertiesRepository.selectByKey(CHAIN_AES_KEY);
								if (keyProp != null && chainKeyMaster.isWrapped(keyProp.getPropertyValue())) {
									String aesKey = chainKeyMaster.unwrap(keyProp.getPropertyValue());
									// 链密钥明文内嵌签发时间戳，消费侧据此做有效期校验
									return cryptoService.encrypt(aesKey, fid + "|" + System.currentTimeMillis());
								}
								// 首次初始化或旧格式迁移：读-改-写序列加锁，避免并发首次访问时重复插入
								synchronized (CHAIN_KEY_LOCK) {
									keyProp = propertiesRepository.selectByKey(CHAIN_AES_KEY);
									if (keyProp == null) {
										String aesKey = cryptoService.generateRandomAesKey();
										Property chainAESKey = new Property();
										chainAESKey.setPropertyKey(CHAIN_AES_KEY);
										chainAESKey.setPropertyValue(chainKeyMaster.wrap(aesKey));
										if (propertiesRepository.insert(chainAESKey) > 0) {
											return cryptoService.encrypt(aesKey, fid + "|" + System.currentTimeMillis());
										}
										return null;
									}
									String aesKey = chainKeyMaster.unwrap(keyProp.getPropertyValue());
									if (!chainKeyMaster.isWrapped(keyProp.getPropertyValue())) {
										keyProp.setPropertyValue(chainKeyMaster.wrap(aesKey));
										propertiesRepository.update(keyProp);
									}
									return cryptoService.encrypt(aesKey, fid + "|" + System.currentTimeMillis());
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
