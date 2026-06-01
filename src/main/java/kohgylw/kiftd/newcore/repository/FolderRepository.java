package kohgylw.kiftd.newcore.repository;

import java.util.List;
import java.util.Map;

import kohgylw.kiftd.server.model.Folder;

public interface FolderRepository {

	Folder selectById(String folderId);

	List<Folder> selectByParentId(String parentId);

	List<Folder> selectByParentIdSection(Map<String, Object> paramMap);

	Folder selectByParentIdAndFolderName(Map<String, String> paramMap);

	int insert(Folder folder);

	int update(Folder folder);

	int deleteById(String folderId);

	long countByParentId(String parentId);
}
