package kohgylw.kiftd.ui.callback;

import java.util.List;

import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.ui.pojo.FileSystemPath;

/**
 *
 * <h2>获取服务器状态回调接口</h2>
 * <p>
 * 该回调接口定义了获取服务器运行时状态的操作契约，包括服务器运行状态、端口号、
 * 缓冲区大小、日志级别、验证码等级、文件系统路径、登录要求、存储扩展区等配置信息。
 * 控制台通过该接口定期刷新显示服务器状态信息。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface GetServerStatus
{
    int getPropertiesStatus();
    
    boolean getServerStatus();
    
    int getPort();
    
    String getInitProt();
    
    int getBufferSize();
    
    String getInitBufferSize();
    
    LogLevel getLogLevel();
    
    LogLevel getInitLogLevel();
    
    VCLevel getVCLevel();
    
    VCLevel getInitVCLevel();
    
    String getFileSystemPath();
    
    String getInitFileSystemPath();
    
    boolean getMustLogin();
    
    boolean isAllowChangePassword();
    
    boolean isOpenFileChain();
    
    List<FileSystemPath> getExtendStores();
    
    int getMaxExtendStoresNum();
}
