package kohgylw.kiftd.printer;

import kohgylw.kiftd.ui.module.*;
import kohgylw.kiftd.server.util.*;

/**
 *
 * <h2>信息输出工具</h2>
 * <p>
 * 该类负责将服务器运行信息输出到控制台或UI界面的日志区域。
 * 支持两种输出模式：UI模式（输出到图形界面的日志文本框）和命令行模式（输出到标准控制台）。
 * 使用单例模式，通过静态方法init()初始化后全局使用。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class Printer
{
    public static Printer instance;
    private static boolean isUIModel;
    private static ServerUIModule sum;
    
    public static void init(final boolean isUIModel) {
        Printer.instance = new Printer();
        if (isUIModel) {
            try {
				Printer.sum = ServerUIModule.getInsatnce();
				Printer.isUIModel = isUIModel;
			} catch (Exception e) {
				System.out.println("错误：无法以UI模式输出信息，自动切换至命令模式输出。详细信息："+e);
			}
        }
    }
    
    public void print(final String context) {
        if (Printer.instance != null) {
            if (Printer.isUIModel) {
                Printer.sum.printMessage(context);
            }
            else {
                System.out.println("[" + new String(ServerTimeUtil.accurateToSecond().getBytes()) + "]" + new String(context.getBytes()) + "\r\n");
            }
        }
    }
}
