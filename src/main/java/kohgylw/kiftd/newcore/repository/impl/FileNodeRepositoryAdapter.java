package kohgylw.kiftd.newcore.repository.impl;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import org.springframework.stereotype.Repository;

@Repository
public class FileNodeRepositoryAdapter implements FileNodeRepository {

	@Resource
	private NodeMapper legacyNodeMapper;

	@Override
	public Node selectById(String fileId) {
		return legacyNodeMapper.selectById(fileId);
	}

	@Override
	public List<Node> selectByParentFolderId(String parentFolderId) {
		return legacyNodeMapper.queryByParentFolderId(parentFolderId);
	}

	@Override
	public List<Node> selectByParentFolderIdSection(Map<String, Object> paramMap) {
		return legacyNodeMapper.queryByParentFolderIdSection(paramMap);
	}

	@Override
	public List<Node> selectByPath(String path) {
		return legacyNodeMapper.queryByPath(path);
	}

	@Override
	public List<Node> selectByPathExcludeById(Map<String, String> paramMap) {
		return legacyNodeMapper.queryByPathExcludeById(paramMap);
	}

	@Override
	public List<Node> selectBySomeFolder(String fileId) {
		return legacyNodeMapper.queryBySomeFolder(fileId);
	}

	@Override
	public int insert(Node node) {
		return legacyNodeMapper.insert(node);
	}

	@Override
	public int update(Node node) {
		return legacyNodeMapper.updateById(node);
	}

	@Override
	public int deleteById(String fileId) {
		return legacyNodeMapper.deleteById(fileId);
	}
}
