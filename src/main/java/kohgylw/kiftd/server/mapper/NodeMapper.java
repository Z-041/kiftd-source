package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface NodeMapper extends BaseMapper<Node> {

	@Select("SELECT * FROM FILE WHERE file_parent_folder = #{pfid} LIMIT 0,2147483647")
	List<Node> queryByParentFolderId(@Param("pfid") final String pfid);

	@Select("SELECT * FROM FILE WHERE file_parent_folder = #{pfid} LIMIT #{offset},#{rows}")
	List<Node> queryByParentFolderIdSection(Map<String, Object> keyMap);

	@Select("SELECT * FROM FILE WHERE file_path = #{path} LIMIT 0,2147483647")
	List<Node> queryByPath(@Param("path") final String path);

	@Select("SELECT * FROM FILE WHERE file_path = #{path} AND file_id <> #{fileId} LIMIT 0,2147483647")
	List<Node> queryByPathExcludeById(Map<String, String> map);

	@Select("SELECT * FROM FILE WHERE file_parent_folder IN (SELECT file_parent_folder FROM FILE WHERE file_id = #{fileId}) LIMIT 0,2147483647")
	List<Node> queryBySomeFolder(@Param("fileId") final String fileId);
}