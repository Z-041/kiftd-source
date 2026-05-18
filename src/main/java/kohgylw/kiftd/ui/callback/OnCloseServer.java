package kohgylw.kiftd.ui.callback;

/**
 *
 * <h2>服务器关闭回调接口</h2>
 * <p>
 * 该回调接口定义了服务器关闭操作的契约，当用户点击控制台的"关闭"按钮或退出程序时触发。
 * 实现类应包含具体的服务器关闭逻辑，并返回关闭结果。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface OnCloseServer
{
	/**
	 * <h2>执行服务器关闭</h2>
	 * <p>由控制台调用，执行服务器的关闭流程。</p>
	 * @return boolean 关闭结果，true表示关闭成功，false表示关闭失败
	 */
    boolean close();
}
