package kohgylw.kiftd.mc;

import kohgylw.kiftd.printer.Printer;

public class MC {

	private static final String VERSION = "1.3.0";
	private static final String APP_NAME = "kiftd";
	private static final String FULL_NAME = "青阳网络文件系统";

	private static final String BANNER = "\r\n"
			+ "  _  ___    __     _    ____   ____  \r\n"
			+ " | |/ / |  / /    / \\  |  _ \\ |  _ \\ \r\n"
			+ " | ' /| | / /    / _ \\ | |_) || | | |\r\n"
			+ " | . \\| |/ /    / ___ \\|  _ < | |_| |\r\n"
			+ " |_|\\_\\___/_/   /_/   \\_\\_| \\_\\|____/ \r\n"
			+ "                                        ";

	public static void main(final String[] args) {
		if (args == null || args.length == 0) {
			startUIMode();
		} else {
			parseAndExecute(args);
		}
	}

	private static void startUIMode() {
		try {
			UIRunner.build();
		} catch (Exception e) {
			String errorMsg = "错误！无法以图形界面模式启动" + APP_NAME + "，您的操作系统可能不支持图形界面。"
					+ "您可以尝试使用命令模式参数 \"-console\" 来启动并开始使用" + APP_NAME + "。";
			if (Printer.instance != null) {
				Printer.instance.print(errorMsg);
			} else {
				System.err.println(errorMsg);
			}
			System.exit(1);
		}
	}

	private static void parseAndExecute(final String[] args) {
		String command = args[0];

		switch (command) {
		case "-h":
		case "--help":
		case "-help":
			printHelp();
			break;
		case "-v":
		case "--version":
		case "-version":
			printVersion();
			break;
		case "--about":
		case "-about":
			printAbout();
			break;
		default:
			ConsoleRunner.build(args);
			break;
		}
	}

	private static void printBanner() {
		System.out.println(BANNER);
		System.out.println("  " + FULL_NAME + " - " + APP_NAME + "  v" + VERSION);
		System.out.println("  基于 Spring Boot 3.4 + Undertow");
		System.out.println();
	}

	private static void printVersion() {
		printBanner();
		System.out.println("  版本: " + VERSION);
		System.out.println("  应用: " + APP_NAME);
		System.out.println("  全称: " + FULL_NAME);
		System.out.println();
		System.out.println("  作者: 青阳龙野(kohgylw)");
		System.out.println("  邮箱: kohgylw@163.com");
		System.out.println();
	}

	private static void printAbout() {
		printBanner();
		System.out.println("  ================================================================");
		System.out.println("  关于 " + FULL_NAME);
		System.out.println("  ================================================================");
		System.out.println();
		System.out.println("  " + APP_NAME + " 是一款轻量级的网络文件系统，");
		System.out.println("  支持文件上传下载、文件夹管理、用户权限控制等功能。");
		System.out.println();
		System.out.println("  主要特性:");
		System.out.println("    - 轻量级，易于部署和使用");
		System.out.println("    - 支持多种数据库（H2/MySQL）");
		System.out.println("    - 完整的用户权限管理系统");
		System.out.println("    - 支持扩展存储区");
		System.out.println("    - 支持 HTTPS 安全访问");
		System.out.println("    - 内置视频转码和图片预览");
		System.out.println("    - 提供图形界面和命令行两种模式");
		System.out.println();
		System.out.println("  技术栈:");
		System.out.println("    - Java 21 + Spring Boot 3.4");
		System.out.println("    - Undertow Web 服务器");
		System.out.println("    - MyBatis Plus 持久层");
		System.out.println("    - H2 / MySQL 数据库");
		System.out.println();
		System.out.println("  作者: 青阳龙野(kohgylw)");
		System.out.println("  邮箱: kohgylw@163.com");
		System.out.println();
		System.out.println("  许可声明:");
		System.out.println("    任何人可以免费获取" + APP_NAME + "的源代码原版拷贝，");
		System.out.println("    并进行分发或修改，可用于任何用途。");
		System.out.println();
	}

	private static void printHelp() {
		printBanner();
		System.out.println("  用法: java -jar " + APP_NAME + ".jar [选项]");
		System.out.println();
		System.out.println("  启动模式:");
		System.out.println("    (无参数)              以图形界面模式启动");
		System.out.println("    -console              以控制台交互模式启动");
		System.out.println("    -start                直接启动服务器引擎");
		System.out.println();
		System.out.println("  文件管理命令:");
		System.out.println("    -import <源路径> <目标路径> [选项]");
		System.out.println("                          将本地文件/文件夹导入到文件系统");
		System.out.println("                          选项: -C 覆盖  -B 保留两者");
		System.out.println("    -export <源路径> <目标路径> [选项]");
		System.out.println("                          将文件系统中的文件/文件夹导出到本地");
		System.out.println("                          选项: -C 覆盖  -B 保留两者");
		System.out.println("    -transfer <扩展区编号> <目标路径>");
		System.out.println("                          移出扩展存储区的数据到指定文件夹");
		System.out.println();
		System.out.println("  账户管理命令:");
		System.out.println("    -account list         列出所有账户");
		System.out.println("    -account add <账户> <密码> [权限]");
		System.out.println("                          添加新账户");
		System.out.println("    -account del <账户>   删除指定账户");
		System.out.println("    -account chpwd <账户> <新密码>");
		System.out.println("                          修改账户密码");
		System.out.println("    -account chauth <账户> <权限>");
		System.out.println("                          修改账户权限");
		System.out.println("    -account info <账户>  查看账户详细信息");
		System.out.println();
		System.out.println("  其他命令:");
		System.out.println("    -resetpwd <账户> <新密码>");
		System.out.println("                          重置账户密码");
		System.out.println();
		System.out.println("  信息查询:");
		System.out.println("    -h, -help, --help     显示此帮助信息");
		System.out.println("    -v, -version, --version");
		System.out.println("                          显示版本信息");
		System.out.println("    -about, --about       显示关于信息");
		System.out.println();
		System.out.println("  示例:");
		System.out.println("    java -jar " + APP_NAME + ".jar                # 图形界面启动");
		System.out.println("    java -jar " + APP_NAME + ".jar -console       # 控制台模式");
		System.out.println("    java -jar " + APP_NAME + ".jar -start         # 直接启动服务器");
		System.out.println("    java -jar " + APP_NAME + ".jar -account list  # 列出账户");
		System.out.println();
		System.out.println("  权限字符说明:");
		System.out.println("    c=新建文件夹  u=上传文件  d=删除");
		System.out.println("    r=重命名      m=移动      l=下载");
		System.out.println();
	}
}
