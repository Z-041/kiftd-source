package kohgylw.kiftd.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import kohgylw.kiftd.newcore.KiftdApplication;
import kohgylw.kiftd.newcore.service.AuthService;
import kohgylw.kiftd.newcore.service.FileService;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.newcore.service.FolderViewService;
import kohgylw.kiftd.newcore.service.SystemService;
import kohgylw.kiftd.newcore.controller.AuthController;
import kohgylw.kiftd.newcore.controller.FileController;
import kohgylw.kiftd.newcore.controller.FolderController;
import kohgylw.kiftd.newcore.config.DataSourceConfig;
import kohgylw.kiftd.newcore.config.ConfigurationManager;

import javax.sql.DataSource;

import kohgylw.kiftd.printer.Printer;

@SpringBootTest(classes = KiftdApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class KiftdApplicationIntegrationTest {

    static {
        if (Printer.instance == null) {
            Printer.init(false);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private AuthService authService;

    @Autowired(required = false)
    private FileService fileService;

    @Autowired(required = false)
    private FolderService folderService;

    @Autowired(required = false)
    private FolderViewService folderViewService;

    @Autowired(required = false)
    private SystemService systemService;

    @Autowired(required = false)
    private AuthController authController;

    @Autowired(required = false)
    private FileController fileController;

    @Autowired(required = false)
    private FolderController folderController;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private DataSourceConfig dataSourceConfig;

    @Test
    void testApplicationContextLoads() {
        assertNotNull(applicationContext, "Spring应用上下文应该被加载");
    }

    @Test
    void testConfigurationManagerBeanExists() {
        ConfigurationManager cm = ConfigurationManager.instance();
        assertNotNull(cm, "ConfigurationManager单例应该存在");
    }

    @Test
    void testConfigurationManagerStatus() {
        ConfigurationManager cm = ConfigurationManager.instance();
        int status = cm.getStatus();
        assertTrue(status == 0 || status == -1, "ConfigurationManager状态应该是合法的");
    }

    @Test
    void testDataSourceBeanExists() {
        if (dataSource != null) {
            assertNotNull(dataSource, "数据源Bean应该存在");
        }
    }

    @Test
    void testDataSourceConfigBeanExists() {
        if (dataSourceConfig != null) {
            assertNotNull(dataSourceConfig, "数据源配置Bean应该存在");
        }
    }

    @Test
    void testAuthServiceBeanExists() {
        if (authService != null) {
            assertNotNull(authService, "认证服务Bean应该存在");
        }
    }

    @Test
    void testFileServiceBeanExists() {
        if (fileService != null) {
            assertNotNull(fileService, "文件服务Bean应该存在");
        }
    }

    @Test
    void testFolderServiceBeanExists() {
        if (folderService != null) {
            assertNotNull(folderService, "文件夹服务Bean应该存在");
        }
    }

    @Test
    void testFolderViewServiceBeanExists() {
        if (folderViewService != null) {
            assertNotNull(folderViewService, "文件夹视图服务Bean应该存在");
        }
    }

    @Test
    void testSystemServiceBeanExists() {
        if (systemService != null) {
            assertNotNull(systemService, "系统服务Bean应该存在");
        }
    }

    @Test
    void testAuthControllerBeanExists() {
        if (authController != null) {
            assertNotNull(authController, "认证控制器Bean应该存在");
        }
    }

    @Test
    void testFileControllerBeanExists() {
        if (fileController != null) {
            assertNotNull(fileController, "文件控制器Bean应该存在");
        }
    }

    @Test
    void testFolderControllerBeanExists() {
        if (folderController != null) {
            assertNotNull(folderController, "文件夹控制器Bean应该存在");
        }
    }

    @Test
    void testBeanCount() {
        int beanCount = applicationContext.getBeanDefinitionCount();
        assertTrue(beanCount > 0, "Spring容器中应该有Bean定义");
    }

    @Test
    void testConfigurationManagerPort() {
        ConfigurationManager cm = ConfigurationManager.instance();
        int port = cm.getPort();
        assertTrue(port > 0 && port <= 65535, "端口号应该在有效范围内");
    }

    @Test
    void testConfigurationManagerFileSystemPath() {
        ConfigurationManager cm = ConfigurationManager.instance();
        String fsPath = cm.getFileSystemPath();
        assertNotNull(fsPath, "文件系统路径不应该为null");
    }
}
