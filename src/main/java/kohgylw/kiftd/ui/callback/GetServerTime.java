package kohgylw.kiftd.ui.callback;

import java.util.Date;

/**
 *
 * <h2>获取服务器时间回调接口</h2>
 * <p>
 * 该回调接口定义了获取服务器当前时间的操作契约，用于在控制台输出信息时显示统一的时间戳。
 * 实现类应返回服务器端的当前时间，以确保日志时间的准确性。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface GetServerTime
{
	/**
	 * <h2>获取当前时间</h2>
	 * <p>获取服务器的当前时间，用于格式化日志输出。</p>
	 * @return Date 服务器的当前日期时间对象
	 */
    Date get();
}
