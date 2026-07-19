package kohgylw.kiftd.newcore.repository.impl;

import kohgylw.kiftd.newcore.repository.PropertiesRepository;
import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Propertie;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class PropertiesRepositoryImpl implements PropertiesRepository {

	private final PropertiesMapper propertiesMapper;

	public PropertiesRepositoryImpl(PropertiesMapper propertiesMapper) {
		this.propertiesMapper = propertiesMapper;
	}

	@Override
	public Propertie selectByKey(String key) {
		return propertiesMapper.selectByKey(key);
	}

	@Override
	public int insert(Propertie propertie) {
		return propertiesMapper.insert(propertie);
	}

	@Override
	public int update(Propertie propertie) {
		return propertiesMapper.update(propertie);
	}

	@Override
	public int deleteByKey(String key) {
		return propertiesMapper.deleteByKey(key);
	}
}
