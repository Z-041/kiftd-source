package kohgylw.kiftd.ui.callback;

import kohgylw.kiftd.server.pojo.*;

/**
 *
 * <h2>更新设置回调接口</h2>
 * <p>
 * 该回调接口定义了更新服务器配置信息的操作契约，当用户在设置窗口中修改配置并点击保存时触发。
 * 实现类应验证配置参数的有效性并将配置持久化到文件中。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface UpdateSetting
{
	/**
	 * <h2>更新服务器设置</h2>
	 * <p>根据传入的服务器设置对象，验证并保存配置信息。</p>
	 * @param s ServerSetting 服务器设置对象，包含所有可配置参数
	 * @return boolean 更新结果，true表示保存成功，false表示保存失败
	 */
    boolean update(final ServerSetting s);
}
