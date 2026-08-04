package kohgylw.kiftd.newcore.config;

import javax.sql.DataSource;

import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import kohgylw.kiftd.server.util.ConfigurationManager;

@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

	@Bean
	public DataSource dataSource(ConfigurationManager cm) {
		final HikariConfig config = new HikariConfig();
		config.setDriverClassName(cm.getFileNodePathDriver());
		config.setJdbcUrl(cm.getFileNodePathURL());
		config.setUsername(cm.getFileNodePathUserName());
		config.setPassword(cm.getFileNodePathPassWord());
		config.setMaximumPoolSize(20);
		config.setMinimumIdle(5);
		config.setConnectionTimeout(30000);
		config.setIdleTimeout(600000);
		config.setMaxLifetime(1800000);
		return new HikariDataSource(config);
	}

	@Bean(name = { "sqlSessionFactory" })
	public MybatisSqlSessionFactoryBean sqlSessionFactoryBean(final DataSource ds) {
		final MybatisSqlSessionFactoryBean ssf = new MybatisSqlSessionFactoryBean();
		ssf.setDataSource(ds);
		return ssf;
	}

	@Bean
	public PlatformTransactionManager transactionManager(final DataSource ds) {
		return new DataSourceTransactionManager(ds);
	}

	@Bean
	public MapperScannerConfigurer mapperScannerConfigurer() {
		final MapperScannerConfigurer msf = new MapperScannerConfigurer();
		msf.setBasePackage("kohgylw.kiftd.server.mapper");
		msf.setSqlSessionFactoryBeanName("sqlSessionFactory");
		return msf;
	}
}
