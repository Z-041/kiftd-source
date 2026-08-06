package kohgylw.kiftd.integration;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.FileNodeUtil;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 集成测试专用配置。
 * <p>
 * 生产环境中，文件节点表（FOLDER/FILE/PROPERTIES）由
 * {@code ServerInitListener}（@WebListener）在 Servlet 容器启动时创建；
 * MOCK 环境下该监听器不会触发，导致集成测试查询报“表不存在”。
 * 此处通过构造器依赖注入保证 {@link ConfigurationManager} 完成配置加载后，
 * 显式初始化文件节点数据库，修复集成测试基建缺陷（技术债：TEST-INFRA-001）。
 * </p>
 */
@Configuration
public class TestConfig {

	public TestConfig(ConfigurationManager cm) {
		// 构造器依赖注入：确保 ConfigurationManager 已完成配置加载（含目录创建与 DB 参数解析）
	}

	@PostConstruct
	public void init() {
		if (Printer.instance == null) {
			Printer.init(false);
		}
		FileNodeUtil.initNodeTableToDataBase();
	}
}
