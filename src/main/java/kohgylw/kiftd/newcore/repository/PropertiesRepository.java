package kohgylw.kiftd.newcore.repository;

import kohgylw.kiftd.server.model.Property;

public interface PropertiesRepository {

	Property selectByKey(String key);

	int insert(Property property);

	int update(Property property);

	int deleteByKey(String key);
}
