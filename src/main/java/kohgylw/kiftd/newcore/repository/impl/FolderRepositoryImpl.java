package kohgylw.kiftd.newcore.repository.impl;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import kohgylw.kiftd.newcore.repository.FolderRepository;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.model.Folder;


@Repository
@Primary
public class FolderRepositoryImpl implements FolderRepository {

	private final FolderMapper folderMapper;

	public FolderRepositoryImpl(FolderMapper folderMapper) {
		this.folderMapper = folderMapper;
	}

	@Override
	public Folder selectById(String folderId) {
		return folderMapper.selectById(folderId);
	}

	@Override
	public List<Folder> selectByParentId(String parentId) {
		return folderMapper.queryByParentId(parentId);
	}

	@Override
	public List<Folder> selectByParentIdSection(Map<String, Object> paramMap) {
		return folderMapper.queryByParentIdSection(paramMap);
	}

	@Override
	public Folder selectByParentIdAndFolderName(Map<String, String> paramMap) {
		return folderMapper.queryByParentIdAndFolderName(paramMap);
	}

	@Override
	public int insert(Folder folder) {
		return folderMapper.insert(folder);
	}

	@Override
	public int update(Folder folder) {
		return folderMapper.updateById(folder);
	}

	@Override
	public int deleteById(String folderId) {
		return folderMapper.deleteById(folderId);
	}

	@Override
	public long countByParentId(String parentId) {
		return folderMapper.countByParentId(parentId);
	}
}
