package kohgylw.kiftd.server.model;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


/**
 *
 * <h2>文件夹数据模型</h2>
 * <p>
 * 该类映射数据库中文件夹表（FOLDER）的数据结构，包含文件夹的唯一标识、
 * 名称、创建日期、创建者、父文件夹ID和访问权限约束等字段。
 * 用于在系统中表示文件夹实体信息，供数据访问层和服务层使用。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@TableName("FOLDER")
public class Folder
{
	@TableId("folder_id")
    private String folderId;
	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private String folderName;
	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private String folderCreationDate;
	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private String folderCreator;
	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private String folderParent;
	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private int folderConstraint;
    
    public String getFolderId() {
        return this.folderId;
    }
    
    public void setFolderId(final String folderId) {
        this.folderId = folderId;
    }
    
    public String getFolderName() {
        return this.folderName;
    }
    
    public void setFolderName(final String folderName) {
        this.folderName = folderName;
    }
    
    public String getFolderCreationDate() {
        return this.folderCreationDate;
    }
    
    public void setFolderCreationDate(final String folderCreationDate) {
        this.folderCreationDate = folderCreationDate;
    }
    
    public String getFolderCreator() {
        return this.folderCreator;
    }
    
    public void setFolderCreator(final String folderCreator) {
        this.folderCreator = folderCreator;
    }
    
    public String getFolderParent() {
        return this.folderParent;
    }
    
    public void setFolderParent(final String folderParent) {
        this.folderParent = folderParent;
    }

	public int getFolderConstraint() {
		return folderConstraint;
	}

	public void setFolderConstraint(int folderConstraint) {
		this.folderConstraint = folderConstraint;
	}
}
