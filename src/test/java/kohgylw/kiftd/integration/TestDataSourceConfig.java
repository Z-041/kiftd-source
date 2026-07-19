package kohgylw.kiftd.integration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@TestConfiguration
@EnableTransactionManagement
public class TestDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:kiftd_test;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    @Bean(name = "sqlSessionFactory")
    @Primary
    public MybatisSqlSessionFactoryBean sqlSessionFactoryBean(final DataSource ds) {
        MybatisSqlSessionFactoryBean ssf = new MybatisSqlSessionFactoryBean();
        ssf.setDataSource(ds);
        return ssf;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(final DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public static MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer msf = new MapperScannerConfigurer();
        msf.setBasePackage("kohgylw.kiftd.server.mapper");
        msf.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return msf;
    }
}
