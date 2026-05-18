package kohgylw.kiftd.server.enumeration;

/**
 *
 * <h2>验证码等级枚举</h2>
 * <p>
 * 该枚举定义了登录验证码的三种等级：
 * Standard（标准验证码，包含字母数字混合）、
 * Simplified（简化验证码，仅包含数字）、
 * Close（关闭验证码功能）。
 * 通过配置文件中的vc参数进行设置。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public enum VCLevel
{
	Standard,
	Simplified,
	Close;
}
