package kohgylw.kiftd.newcore.infrastructure.storage.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kohgylw.kiftd.newcore.infrastructure.storage.FileStorageService;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.FileBlockUtil;

import java.io.File;
import java.util.List;

@Service
@Primary
public class FileStorageServiceImpl implements FileStorageService {

	private final FileBlockUtil fileBlockUtil;

	public FileStorageServiceImpl(FileBlockUtil fileBlockUtil) {
		this.fileBlockUtil = fileBlockUtil;
	}

	@Override
	public File saveMultipartFile(MultipartFile file) {
		return fileBlockUtil.saveToFileBlocks(file);
	}

	@Override
	public File saveFile(File file) {
		return fileBlockUtil.saveToFileBlocks(file);
	}

	@Override
	public boolean deleteNode(Node node) {
		return fileBlockUtil.deleteNode(node);
	}

	@Override
	public File getFileFromBlocks(Node node) {
		return fileBlockUtil.getFileFromBlocks(node);
	}

	@Override
	public void checkFileBlocks() {
		fileBlockUtil.checkFileBlocks();
	}

	@Override
	public void initTempDir() {
		fileBlockUtil.initTempDir();
	}

	@Override
	public String getFileSize(long size) {
		return fileBlockUtil.getFileSize(size);
	}

	@Override
	public Node insertNewNode(String fileName, String account, String filePath, String fileSize,
			String fileParentFolder) {
		return fileBlockUtil.insertNewNode(fileName, account, filePath, fileSize, fileParentFolder);
	}

	@Override
	public boolean isValidNode(Node node) {
		return fileBlockUtil.isValidNode(node);
	}

	@Override
	public String getNodePath(Node node) {
		return fileBlockUtil.getNodePath(node);
	}

	@Override
	public String getETag(File block) {
		return fileBlockUtil.getETag(block);
	}

	@Override
	public String createZip(List<String> idList, List<String> fidList, String account) {
		return fileBlockUtil.createZip(idList, fidList, account);
	}
}
