package kohgylw.kiftd.server.pojo;

import java.util.*;

/**
 *
 * <h2>图片预览列表封装类</h2>
 * <p>
 * 该类用于封装图片预览模式的视图数据，包含当前文件夹下所有图片的信息列表
 * 以及当前查看图片在列表中的索引位置，供前端实现图片切换浏览功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class PictureViewList
{
    private List<PictureInfo> pictureViewList;
    private int index;
    
    public List<PictureInfo> getPictureViewList() {
        return this.pictureViewList;
    }
    
    public void setPictureViewList(final List<PictureInfo> pictureViewList) {
        this.pictureViewList = pictureViewList;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    public void setIndex(final int index) {
        this.index = index;
    }
}
