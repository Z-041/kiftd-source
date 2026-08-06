package kohgylw.kiftd.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import kohgylw.kiftd.server.model.Property;


public interface PropertiesMapper extends BaseMapper<Property> {

	default Property selectByKey(String key) {
		return selectById(key);
	}

	default int deleteByKey(String key) {
		return deleteById(key);
	}

	default int update(Property p) {
		return updateById(p);
	}
}
