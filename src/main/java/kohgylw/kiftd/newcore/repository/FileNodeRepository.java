package kohgylw.kiftd.newcore.repository;

import java.util.List;
import java.util.Map;

import kohgylw.kiftd.server.model.Node;

public interface FileNodeRepository {

	Node selectById(String fileId);

	List<Node> selectByParentFolderId(String parentFolderId);

	List<Node> selectByParentFolderIdSection(Map<String, Object> paramMap);

	List<Node> selectByPath(String path);

	List<Node> selectByPathExcludeById(Map<String, String> paramMap);

	List<Node> selectBySomeFolder(String fileId);

	int insert(Node node);

	int update(Node node);

	int deleteById(String fileId);
}
