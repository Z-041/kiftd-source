package kohgylw.kiftd.server.mapper;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import kohgylw.kiftd.server.model.Folder;


public interface FolderMapper extends BaseMapper<Folder> {

	@Select("SELECT * FROM FOLDER WHERE folder_parent = #{pid} LIMIT #{offset},#{rows}")
	List<Folder> queryByParentIdSection(Map<String, Object> keyMap);

	@Select("SELECT * FROM FOLDER WHERE folder_parent = #{pid} LIMIT 0,2147483647")
	List<Folder> queryByParentId(@Param("pid") final String pid);

	@Select("<script>SELECT * FROM FOLDER WHERE folder_parent IN <foreach item='pid' index='index' collection='pids' open='(' separator=',' close=')'>#{pid}</foreach> LIMIT 0,2147483647</script>")
	List<Folder> queryByParentIds(@Param("pids") List<String> pids);

	@Select("SELECT * FROM FOLDER WHERE folder_parent = #{parentId} AND folder_name = #{folderName} LIMIT 0,2147483647")
	Folder queryByParentIdAndFolderName(Map<String, String> map);

	@Select("SELECT COUNT(*) FROM FOLDER WHERE folder_parent = #{pid}")
	long countByParentId(@Param("pid") final String pid);
}
