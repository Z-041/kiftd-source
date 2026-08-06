package kohgylw.kiftd.newcore.repository.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import kohgylw.kiftd.newcore.repository.PropertiesRepository;
import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Property;


@Repository
@Primary
public class PropertiesRepositoryImpl implements PropertiesRepository {

	private final PropertiesMapper propertiesMapper;

	public PropertiesRepositoryImpl(PropertiesMapper propertiesMapper) {
		this.propertiesMapper = propertiesMapper;
	}

	@Override
	public Property selectByKey(String key) {
		return propertiesMapper.selectByKey(key);
	}

	@Override
	public int insert(Property property) {
		return propertiesMapper.insert(property);
	}

	@Override
	public int update(Property property) {
		return propertiesMapper.update(property);
	}

	@Override
	public int deleteByKey(String key) {
		return propertiesMapper.deleteByKey(key);
	}
}
