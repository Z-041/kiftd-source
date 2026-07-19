package kohgylw.kiftd.mc;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kohgylw.kiftd.newcore.KiftdApplication;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.util.file_system_manager.FileSystemManager;

public class ConsoleRunner {

	private static volatile ConsoleRunner instance;
	private static KiftdApplication app;

	private final ExecutorService worker;
	private final Scanner reader;
	private final FileSystemCommandHandler fsHandler;
	private final AccountCommandHandler accountHandler;

	private static final String COMMAND_TIPS = "kiftd:您可以输入以下指令以控制服务器：\r\n"
			+ "-start 启动服务器\r\n"
			+ "-stop 停止服务器\r\n"
			+ "-exit 停止服务器并退出应用\r\n"
			+ "-restart 重启服务器\r\n"
			+ "-files 文件管理\r\n"
			+ "-status 查看服务器状态\r\n"
			+ "-help 显示帮助文本";

	private ConsoleRunner() {
		Printer.init(false);
		app = new KiftdApplication();
		worker = Executors.newSingleThreadExecutor();
		reader = new Scanner(System.in);

		accountHandler = new AccountCommandHandler();
		fsHandler = new FileSystemCommandHandler(
				() -> reader,
				new FileSystemCommandHandler.ProgressListenerFactory() {
					@Override
					public Runnable create() {
						ProgressListener pl = new ProgressListener();
						worker.execute(pl);
						return pl;
					}

					@Override
					public void stop(Runnable listener) {
						if (listener instanceof ProgressListener) {
							((ProgressListener) listener).stop();
						}
					}
				});
	}

	public static ConsoleRunner build(final String[] args) {
		if (instance == null) {
			synchronized (ConsoleRunner.class) {
				if (instance == null) {
					instance = new ConsoleRunner();
				}
			}
		}
		instance.execute(args);
		return instance;
	}

	private void execute(final String[] args) {
		if (args.length > 0) {
			final String command = args[0];
			switch (command) {
			case "-export":
				fsHandler.doExport(args);
				break;
			case "-import":
				fsHandler.doImport(args);
				break;
			case "-transfer":
				fsHandler.doTransfer(args);
				break;
			case "-console":
				startKiftdByConsole();
				break;
			case "-start":
				app.start();
				break;
			case "-resetpwd":
				accountHandler.resetPassword(args);
				break;
			case "-account":
				accountHandler.handleAccountCommand(args);
				break;
			default:
				Printer.instance.print("kiftd:无效的指令，使用控制台模式启动请输入参数 -console，直接启动服务器引擎请输入参数 -start，使用UI模式启动请不要传入任何参数。");
				break;
			}
		}
	}

	private void startKiftdByConsole() {
		Printer.instance.print(" 青阳网络文件系统-kiftd 控制台模式[Console model]");
		Printer.instance.print("Character encoding with UTF-8");
		final Thread t = new Thread(() -> {
			Printer.instance.print("正在初始化服务器...");
			if (ConfigurationManager.instance().getStatus() == 0) {
				awaiting();
			}
		});
		t.start();
	}

	private void awaiting() {
		Thread t = new Thread(() -> {
			Printer.instance.print("命令帮助：\r\n" + COMMAND_TIPS + "\r\n");
			try {
				while (true) {
					Printer.instance.print("kiftd: console$ ");
					String command = reader.nextLine().trim();
					if (handleConsoleCommand(command)) {
						return;
					}
				}
			} catch (Exception e) {
				Printer.instance.print(e.toString());
				Printer.instance.print("错误：读取命令时出现意外导致程序退出，请重启kiftd。");
			}
		});
		t.start();
	}

	private boolean handleConsoleCommand(String command) {
		switch (command) {
		case "":
			return false;
		case "-start":
			startServer();
			return false;
		case "-stop":
			stopServer();
			return false;
		case "-restart":
			restartServer();
			return false;
		case "-status":
			printServerStatus();
			return false;
		case "-files":
			fsHandler.enterFileManager();
			return false;
		case "-exit":
			reader.close();
			exit();
			return true;
		case "help":
		case "--help":
		case "-help":
			Printer.instance.print("命令帮助：\r\n" + COMMAND_TIPS);
			return false;
		default:
			Printer.instance.print("错误：无法识别的指令。\r\n" + COMMAND_TIPS);
			return false;
		}
	}

	private void startServer() {
		Printer.instance.print("执行命令：启动服务器...");
		if (app.isRunning()) {
			Printer.instance.print("错误：服务器已经启动了。您可以使用 -status 命令查看服务器运行状态或使用 -stop 命令停止服务器。");
		} else if (app.start()) {
			Printer.instance.print("kiftd服务器已启动，可以正常访问了，您可以使用 -status 指令查看运行状态。");
		} else {
			printStartupError();
		}
	}

	private void printStartupError() {
		if (ConfigurationManager.instance().getStatus() != 0) {
			switch (ConfigurationManager.instance().getStatus()) {
			case ConfigurationManager.INVALID_PORT:
				Printer.instance.print("错误：kiftd服务器未能启动，端口设置无效。");
				break;
			case ConfigurationManager.INVALID_BUFFER_SIZE:
				Printer.instance.print("错误：kiftd服务器未能启动，缓冲区设置无效。");
				break;
			case ConfigurationManager.INVALID_FILE_SYSTEM_PATH:
				Printer.instance.print("错误：kiftd服务器未能启动，文件系统路径或某一扩展存储区设置无效。");
				break;
			case ConfigurationManager.INVALID_LOG:
				Printer.instance.print("错误：kiftd服务器未能启动，日志等级设置无效。");
				break;
			case ConfigurationManager.INVALID_VC:
				Printer.instance.print("错误：kiftd服务器未能启动，登录验证码设置无效。");
				break;
			default:
				Printer.instance.print("错误：kiftd服务器未能启动，请重试或检查设置。");
				break;
			}
		} else {
			Printer.instance.print("错误：kiftd服务器未能启动，请重试或检查设置。");
		}
	}

	private void exit() {
		Printer.instance.print("执行命令：停止服务器并退出kiftd...");
		if (app.isRunning() && app.stop()) {
			Printer.instance.print("服务器已关闭，停止所有访问。");
		}
		worker.shutdown();
		if (reader != null) {
			reader.close();
		}
		Printer.instance.print("退出应用。");
		System.exit(0);
	}

	private void restartServer() {
		Printer.instance.print("执行命令：重启服务器...");
		if (app.isRunning()) {
			if (app.stop()) {
				if (app.start()) {
					Printer.instance.print("服务器重启成功，可以正常访问了。");
				} else {
					Printer.instance.print("错误：无法重新启动服务器，请尝试手动启动。");
				}
			} else {
				Printer.instance.print("错误：无法关闭服务器，请尝试手动关闭。");
			}
		} else {
			Printer.instance.print("错误：服务器尚未启动。您可以使用 -start 命令启动服务器或使用 -status 命令查看服务器运行状态。");
		}
	}

	private void stopServer() {
		Printer.instance.print("执行命令：停止服务器...");
		if (app.isRunning()) {
			if (app.stop()) {
				Printer.instance.print("服务器已关闭，停止所有访问。");
			} else {
				Printer.instance.print("错误：无法关闭服务器，您可以尝试强制关闭。");
			}
		} else {
			Printer.instance.print("错误：服务器尚未启动。您可以使用 -start 命令启动服务器或使用 -exit 命令退出应用。");
		}
	}

	private void printServerStatus() {
		Printer.instance.print("服务器状态：\r\n"
				+ "<Port>端口号:" + ConfigurationManager.instance().getPort() + "\r\n"
				+ "<LogLevel>日志等级:" + ConfigurationManager.instance().getLogLevel() + "\r\n"
				+ "<BufferSize>缓冲区大小:" + ConfigurationManager.instance().getBuffSize() + " B\r\n"
				+ "<FileSystemPath>文件系统存储路径：" + ConfigurationManager.instance().getFileSystemPath() + "\r\n"
				+ "<MustLogin>是否必须登录：" + ConfigurationManager.instance().mustLogin() + "\r\n"
				+ "<Running>运行状态：" + app.isRunning());
	}

	class ProgressListener implements Runnable {

		private volatile boolean running = true;

		@Override
		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			while (running) {
				Printer.instance.print(FileSystemManager.message);
				Printer.instance.print("当前进度：" + FileSystemManager.per + "%");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		public void stop() {
			running = false;
		}
	}
}
