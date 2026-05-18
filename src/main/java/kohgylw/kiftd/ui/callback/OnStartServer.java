package kohgylw.kiftd.ui.callback;

/**
 *
 * <h2>服务器启动回调接口</h2>
 * <p>
 * 该回调接口定义了服务器启动操作的契约，当用户点击控制台的"开启"按钮时触发。
 * 实现类应包含具体的服务器启动逻辑，并返回启动结果。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface OnStartServer
{
	/**
	 * <h2>执行服务器启动</h2>
	 * <p>由控制台调用，执行服务器的启动流程。</p>
	 * @return boolean 启动结果，true表示启动成功，false表示启动失败
	 */
    boolean start();
}
