package kohgylw.kiftd.server.mapper;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import kohgylw.kiftd.server.model.Node;


public interface NodeMapper extends BaseMapper<Node> {

	@Select("SELECT * FROM FILE WHERE file_parent_folder = #{pfid} LIMIT 0,2147483647")
	List<Node> queryByParentFolderId(@Param("pfid") final String pfid);

	@Select("<script>SELECT * FROM FILE WHERE <choose><when test='pfids != null and pfids.size() > 0'>file_parent_folder IN <foreach item='pfid' index='index' collection='pfids' open='(' separator=',' close=')'>#{pfid}</foreach></when><otherwise>1 = 0</otherwise></choose> LIMIT 0,2147483647</script>")
	List<Node> queryByParentFolderIds(@Param("pfids") List<String> pfids);

	@Select("<script>SELECT * FROM FILE WHERE <choose><when test='pfids != null and pfids.size() > 0'>file_parent_folder IN <foreach item='pfid' index='index' collection='pfids' open='(' separator=',' close=')'>#{pfid}</foreach></when><otherwise>1 = 0</otherwise></choose> LIMIT #{limit}</script>")
	List<Node> queryByParentFolderIdsLimit(@Param("pfids") List<String> pfids, @Param("limit") int limit);

	@Select("SELECT * FROM FILE WHERE file_parent_folder = #{pfid} LIMIT #{offset},#{rows}")
	List<Node> queryByParentFolderIdSection(Map<String, Object> keyMap);

	@Select("SELECT * FROM FILE WHERE file_path = #{path} LIMIT 0,2147483647")
	List<Node> queryByPath(@Param("path") final String path);

	@Select("SELECT * FROM FILE WHERE file_path = #{path} AND file_id <> #{fileId} LIMIT 0,2147483647")
	List<Node> queryByPathExcludeById(Map<String, String> map);

	@Select("SELECT * FROM FILE WHERE file_parent_folder IN (SELECT file_parent_folder FROM FILE WHERE file_id = #{fileId}) LIMIT 0,2147483647")
	List<Node> queryBySomeFolder(@Param("fileId") final String fileId);

	@Select("SELECT COUNT(*) FROM FILE WHERE file_parent_folder = #{pfid}")
	long countByParentFolderId(@Param("pfid") final String pfid);
}
