package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.Propertie;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface PropertiesMapper extends BaseMapper<Propertie> {

	default Propertie selectByKey(String key) {
		return selectById(key);
	}

	default int deleteByKey(String key) {
		return deleteById(key);
	}

	default int update(Propertie p) {
		return updateById(p);
	}
}