package kohgylw.kiftd.printer;

/**
 *
 * <h2>输出消息接收器</h2>
 * <p>
 * 定义服务器运行信息的输出目标（PKG-004）。{@link Printer} 仅依赖本接口完成消息输出，
 * 不再反向依赖具体的 Swing UI 模块，从而保证 printer 作为核心基础设施可脱离图形界面独立使用。
 * </p>
 *
 * @author 技术债治理迭代
 * @version 1.0
 */
public interface MessageOutput {

	/**
	 * 输出一条运行消息。
	 *
	 * @param context String 消息内容
	 */
	void printMessage(String context);
}
