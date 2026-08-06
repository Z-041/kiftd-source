package kohgylw.kiftd.printer;

import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.util.ServerTimeUtil;


public class Printer {

	public static volatile Printer instance;

	private static volatile boolean isUIModel;
	private static volatile MessageOutput messageOutput;
	private static volatile LogLevel logLevel = LogLevel.Event;

	private Printer() {
	}

	public static synchronized void init(final boolean uiModel) {
		if (instance == null) {
			instance = new Printer();
		}
		isUIModel = uiModel;
	}

	/**
	 * 注册消息输出目标（PKG-004：Printer 不再反向依赖具体 UI 模块，由启动器注入输出接收器）。
	 *
	 * @param out MessageOutput 消息输出接收器（如 Swing 主界面）；传 null 时回退到控制台输出
	 */
	public static void setMessageOutput(final MessageOutput out) {
		messageOutput = out;
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
		if (isUIModel && messageOutput != null) {
			messageOutput.printMessage(context);
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
