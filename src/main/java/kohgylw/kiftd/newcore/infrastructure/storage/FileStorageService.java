package kohgylw.kiftd.newcore.infrastructure.storage;

import java.io.File;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import kohgylw.kiftd.server.model.Node;

public interface FileStorageService {

	File saveMultipartFile(MultipartFile file);

	File saveFile(File file);

	boolean deleteNode(Node node);

	File getFileFromBlocks(Node node);

	void checkFileBlocks();

	void initTempDir();

	String getFileSize(long size);

	Node insertNewNode(String fileName, String account, String filePath, String fileSize, String fileParentFolder);

	boolean isValidNode(Node node);

	String getNodePath(Node node);

	String getETag(File block);

	String createZip(List<String> idList, List<String> fidList, String account);
}
