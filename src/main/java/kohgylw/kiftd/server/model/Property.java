package kohgylw.kiftd.server.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 *
 * <h2>文件系统相关设置项的模型</h2>
 * <p>该模型用于描述文件系统数据库中的PROPERTIES表。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@TableName("PROPERTIES")
public class Property {

	@TableId("propertie_key")
	private String propertyKey;
	@TableField("propertie_value")
	private String propertyValue;

	public String getPropertyKey() {
		return propertyKey;
	}
	public void setPropertyKey(String propertyKey) {
		this.propertyKey = propertyKey;
	}
	public String getPropertyValue() {
		return propertyValue;
	}
	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

}
