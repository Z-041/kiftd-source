package kohgylw.kiftd.server.configation;

import javax.sql.*;
import com.zaxxer.hikari.*;
import kohgylw.kiftd.server.util.*;
import org.springframework.context.annotation.*;
import org.mybatis.spring.*;
import org.springframework.beans.factory.annotation.*;
import org.mybatis.spring.mapper.*;
import java.io.*;
import org.springframework.core.io.*;

/**
 * 
 * <h2>服务器部分数据接入设置</h2>
 * <p>
 * 该配置类定义了服务器组件使用的MyBatis将如何链接数据库。如需更换其他数据库，请在此配置自己的数据源并替换原有数据源。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Configurable
public class DataAccess {
	private static Resource[] mapperFiles;
	private static Resource mybatisConfg;

	@Bean
	public DataSource dataSource() {
		final HikariConfig config = new HikariConfig();
		config.setDriverClassName(ConfigureReader.instance().getFileNodePathDriver());
		config.setJdbcUrl(ConfigureReader.instance().getFileNodePathURL());
		config.setUsername(ConfigureReader.instance().getFileNodePathUserName());
		config.setPassword(ConfigureReader.instance().getFileNodePathPassWord());
		config.setMaximumPoolSize(20);
		config.setMinimumIdle(5);
		config.setConnectionTimeout(30000);
		config.setIdleTimeout(600000);
		config.setMaxLifetime(1800000);
		return new HikariDataSource(config);
	}

	@Bean(name = { "sqlSessionFactory" })
	@Autowired
	public SqlSessionFactoryBean sqlSessionFactoryBean(final DataSource ds) {
		final SqlSessionFactoryBean ssf = new SqlSessionFactoryBean();
		ssf.setDataSource(ds);
		ssf.setConfigLocation(DataAccess.mybatisConfg);
		ssf.setMapperLocations(DataAccess.mapperFiles);
		return ssf;
	}

	@Bean
	public MapperScannerConfigurer mapperScannerConfigurer() {
		final MapperScannerConfigurer msf = new MapperScannerConfigurer();
		msf.setBasePackage("kohgylw.kiftd.server.mapper");
		msf.setSqlSessionFactoryBeanName("sqlSessionFactory");
		return msf;
	}

	static {
		final String mybatisResourceFolder = ConfigureReader.instance().getPath() + File.separator + "mybatisResource"
				+ File.separator;
		final String mapperFilesFolder = mybatisResourceFolder + "mapperXML" + File.separator;
		DataAccess.mapperFiles = new Resource[] { new FileSystemResource(mapperFilesFolder + "NodeMapper.xml"),
				new FileSystemResource(mapperFilesFolder + "FolderMapper.xml"),
				new FileSystemResource(mapperFilesFolder + "PropertiesMapper.xml") };
		DataAccess.mybatisConfg = (Resource) new FileSystemResource(mybatisResourceFolder + "mybatis.xml");
	}
}
