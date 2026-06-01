package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;
import java.util.*;
import java.text.*;

/**
 *
 * <h2>服务器信息服务实现类</h2>
 * <p>
 * 该类实现了 ServerInfoService 接口中定义的服务器信息查询业务逻辑，
 * 包括获取服务器操作系统名称和当前服务器时间等功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.server.service.ServerInfoService
 */
@Service
public class ServerInfoServiceImpl implements ServerInfoService
{
    @Override
    public String getOSName() {
        return System.getProperty("os.name");
    }
    
    @Override
    public String getServerTime() {
        final Date d = new Date();
        final DateFormat df = new SimpleDateFormat("yyyy\u5e74MM\u6708dd\u65e5 HH:mm");
        return df.format(d);
    }
}
