package kohgylw.kiftd.newcore.repository.impl;

import java.util.List;
import java.util.Map;

import kohgylw.kiftd.newcore.repository.FileNodeRepository;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class FileNodeRepositoryImpl implements FileNodeRepository {

	private final NodeMapper nodeMapper;

	public FileNodeRepositoryImpl(NodeMapper nodeMapper) {
		this.nodeMapper = nodeMapper;
	}

	@Override
	public Node selectById(String fileId) {
		return nodeMapper.selectById(fileId);
	}

	@Override
	public List<Node> selectByParentFolderId(String parentFolderId) {
		return nodeMapper.queryByParentFolderId(parentFolderId);
	}

	@Override
	public List<Node> selectByParentFolderIds(List<String> parentFolderIds) {
		return nodeMapper.queryByParentFolderIds(parentFolderIds);
	}

	@Override
	public List<Node> selectByParentFolderIdSection(Map<String, Object> paramMap) {
		return nodeMapper.queryByParentFolderIdSection(paramMap);
	}

	@Override
	public List<Node> selectByPath(String path) {
		return nodeMapper.queryByPath(path);
	}

	@Override
	public List<Node> selectByPathExcludeById(Map<String, String> paramMap) {
		return nodeMapper.queryByPathExcludeById(paramMap);
	}

	@Override
	public List<Node> selectBySomeFolder(String fileId) {
		return nodeMapper.queryBySomeFolder(fileId);
	}

	@Override
	public int insert(Node node) {
		return nodeMapper.insert(node);
	}

	@Override
	public int update(Node node) {
		return nodeMapper.updateById(node);
	}

	@Override
	public int deleteById(String fileId) {
		return nodeMapper.deleteById(fileId);
	}

	@Override
	public long countByParentFolderId(String parentFolderId) {
		return nodeMapper.countByParentFolderId(parentFolderId);
	}
}
