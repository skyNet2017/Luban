package top.zibin.luban;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.hss01248.luban.showresult.ShowResultUtil;

import java.io.File;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public interface ILubanConfig {

   default void reportException(Throwable throwable){
       if (LubanUtil.enableLog) {
           LogUtils.w(throwable);
       }
   }

    /**
     * 使用应用内部的路径,避免被app提示
     *
     * @return
     */
   default File getSaveDir(){
       File dir = LubanUtil.app.getFilesDir();
       File subDir = new File(dir, "luban");
       if (!subDir.exists()) {
           subDir.mkdirs();
       }
       return subDir;
   }

    /**
     * 耗时和压缩百分比,上报到firebase的trace
     *
     * @param timeCost
     * @param percent
     */
    default void trace(String inputPath, String outputPath, long timeCost, int percent, long sizeAfterCompressInK, long width, long height) {
        LubanUtil.i(inputPath + "\ncompress to --> " + outputPath + "\ntime cost : " + timeCost + "ms, filesize after compress:"
                + sizeAfterCompressInK + "kB , 减少掉:" + percent + "%,  wh:" + width + "x" + height);
        ShowResultUtil.showResult(ActivityUtils.getTopActivity(), inputPath, outputPath, timeCost, percent, sizeAfterCompressInK, width, height);
    }

    /**
     *
     * @param exif null时返回要不要编辑的判断,不为null时则正式编辑内容.
     * @return
     */
    default boolean editExif(@Nullable ExifInterface exif) {
        return false;
    }
}
