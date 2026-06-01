package kohgylw.kiftd.newcore.repository.impl;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.model.Folder;
import org.springframework.stereotype.Repository;

@Repository
public class FolderRepositoryAdapter implements FolderRepository {

	@Resource
	private FolderMapper legacyFolderMapper;

	@Override
	public Folder selectById(String folderId) {
		return legacyFolderMapper.selectById(folderId);
	}

	@Override
	public List<Folder> selectByParentId(String parentId) {
		return legacyFolderMapper.queryByParentId(parentId);
	}

	@Override
	public List<Folder> selectByParentIdSection(Map<String, Object> paramMap) {
		return legacyFolderMapper.queryByParentIdSection(paramMap);
	}

	@Override
	public Folder selectByParentIdAndFolderName(Map<String, String> paramMap) {
		return legacyFolderMapper.queryByParentIdAndFolderName(paramMap);
	}

	@Override
	public int insert(Folder folder) {
		return legacyFolderMapper.insert(folder);
	}

	@Override
	public int update(Folder folder) {
		return legacyFolderMapper.updateById(folder);
	}

	@Override
	public int deleteById(String folderId) {
		return legacyFolderMapper.deleteById(folderId);
	}

	@Override
	public long countByParentId(String parentId) {
		List<Folder> list = legacyFolderMapper.queryByParentId(parentId);
		return list != null ? list.size() : 0L;
	}
}
