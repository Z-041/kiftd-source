package kohgylw.kiftd.newcore.repository;

import kohgylw.kiftd.server.model.Propertie;

public interface PropertiesRepository {

	Propertie selectByKey(String key);

	int insert(Propertie propertie);

	int update(Propertie propertie);

	int deleteByKey(String key);
}
