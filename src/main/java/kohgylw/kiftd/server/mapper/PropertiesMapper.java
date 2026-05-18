package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.Propertie;

/**
 *
 * <h2>系统属性数据访问接口</h2>
 * <p>
 * 该数据访问接口定义了系统属性配置表（kiftd_properties）的CRUD操作方法，
 * 包括插入、按key删除、按key查询和更新属性记录。属性表以键值对的形式存储系统配置信息。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface PropertiesMapper {
	
	int insert(final Propertie p);
	
	int deleteByKey(final String propertieKey);
	
	Propertie selectByKey(final String propertieKey);
	
	int update(final Propertie p);
	
}
