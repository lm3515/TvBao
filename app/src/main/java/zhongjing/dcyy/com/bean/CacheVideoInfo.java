package zhongjing.dcyy.com.bean;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by Dcyy on 2017/1/23.
 */

public class CacheVideoInfo implements Serializable{
    public Bitmap videoBitmap;  //视频第一帧
    public long videoSize; //视频大小
    public String videoName;    //视频名称

    @Override
    public String toString() {
        return "videoName ="+videoName+"videoSize = "+videoSize;
    }
}
