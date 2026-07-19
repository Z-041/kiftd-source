package kohgylw.kiftd.printer;

import kohgylw.kiftd.ui.module.ServerUIModule;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.enumeration.LogLevel;

public class Printer {

	public static volatile Printer instance;

	private static volatile boolean isUIModel;
	private static volatile ServerUIModule sum;
	private static volatile LogLevel logLevel = LogLevel.Event;

	private Printer() {
	}

	public static synchronized void init(final boolean uiModel) {
		if (instance == null) {
			instance = new Printer();
		}
		isUIModel = uiModel;
		if (uiModel) {
			try {
				sum = ServerUIModule.getInsatnce();
				System.err.println("[STARTUP] Printer got ServerUIModule instance: " + (sum != null));
			} catch (Exception e) {
				isUIModel = false;
				System.err.println("[STARTUP] Printer FAILED to get ServerUIModule: " + e);
				instance.print("错误：无法以UI模式输出信息，自动切换至命令模式输出。详细信息：" + e);
			}
		}
	}

	public static void setLogLevel(LogLevel level) {
		if (level != null) {
			logLevel = level;
		}
	}

	public static LogLevel getLogLevel() {
		return logLevel;
	}

	public void print(final String context) {
		if (context == null) {
			return;
		}
		if (isUIModel && sum != null) {
			sum.printMessage(context);
		} else {
			System.out.println("[" + ServerTimeUtil.accurateToSecond() + "]" + context);
		}
	}

	public void info(final String context) {
		if (shouldLog(LogLevel.Event)) {
			print("[INFO] " + context);
		}
	}

	public void warn(final String context) {
		if (shouldLog(LogLevel.Runtime_Exception)) {
			print("[WARN] " + context);
		}
	}

	public void error(final String context) {
		if (shouldLog(LogLevel.Runtime_Exception)) {
			print("[ERROR] " + context);
		}
	}

	public void debug(final String context) {
		if (shouldLog(LogLevel.Event)) {
			print("[DEBUG] " + context);
		}
	}

	public void success(final String context) {
		if (shouldLog(LogLevel.Event)) {
			print("[OK] " + context);
		}
	}

	private boolean shouldLog(LogLevel level) {
		if (logLevel == null || level == null) {
			return true;
		}
		switch (logLevel) {
		case None:
			return false;
		case Runtime_Exception:
			return level == LogLevel.Runtime_Exception;
		case Event:
			return true;
		default:
			return true;
		}
	}

	public void printf(final String format, final Object... args) {
		if (format == null) {
			return;
		}
		print(String.format(format, args));
	}

	public void printSeparator() {
		print("----------------------------------------");
	}

	public void printHeader(final String title) {
		printSeparator();
		print("  " + title);
		printSeparator();
	}
}
