package kohgylw.kiftd.server.enumeration;

/**
 *
 * <h2>日志级别枚举</h2>
 * <p>
 * 该枚举定义了系统日志记录的三种级别：
 * None（不记录任何日志）、
 * Runtime_Exception（仅记录运行时异常）、
 * Event（记录所有事件）。
 * 通过配置文件中的log参数进行设置。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public enum LogLevel
{
    None, 
    Runtime_Exception, 
    Event;
}
