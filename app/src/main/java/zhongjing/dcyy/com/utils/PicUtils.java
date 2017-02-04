package zhongjing.dcyy.com.utils;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Dcyy on 2017/1/16.
 */

public class PicUtils {
    /** 首先默认个文件保存路径 */
    private static final String SAVE_PIC_PATH= Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/mnt/sdcard";//保存到SD卡
    public static final String SAVE_REAL_PATH = SAVE_PIC_PATH+ "/zhongjing";//保存的确切位置

    public static void saveBitmap(Bitmap bm, String fileName) {
        File filePath = new File(SAVE_REAL_PATH);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        File picPath = new File(filePath, fileName);
        try {
            filePath.createNewFile();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(picPath));
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
