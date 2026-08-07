package kohgylw.kiftd.mc;

import kohgylw.kiftd.printer.Printer;

public class MC {

	private static final String APP_NAME = "kiftd";

	public static void main(final String[] args) {
		startUIMode();
	}

	private static void startUIMode() {
		try {
			UIRunner.build();
		} catch (Exception e) {
			String errorMsg = "错误！无法以图形界面模式启动" + APP_NAME + "，您的操作系统可能不支持图形界面。";
			if (Printer.instance != null) {
				Printer.instance.print(errorMsg);
			} else {
				System.err.println(errorMsg);
			}
			System.exit(1);
		}
	}
}
