package kohgylw.kiftd.newcore.repository.impl;

import jakarta.annotation.Resource;
import kohgylw.kiftd.newcore.repository.PropertiesRepository;
import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Propertie;
import org.springframework.stereotype.Repository;

@Repository
public class PropertiesRepositoryAdapter implements PropertiesRepository {

	@Resource
	private PropertiesMapper legacyPropertiesMapper;

	@Override
	public Propertie selectByKey(String key) {
		return legacyPropertiesMapper.selectByKey(key);
	}

	@Override
	public int insert(Propertie propertie) {
		return legacyPropertiesMapper.insert(propertie);
	}

	@Override
	public int update(Propertie propertie) {
		return legacyPropertiesMapper.update(propertie);
	}

	@Override
	public int deleteByKey(String key) {
		return legacyPropertiesMapper.deleteByKey(key);
	}
}
