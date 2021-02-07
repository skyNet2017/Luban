package top.zibin.luban;

import android.os.Environment;

import java.io.File;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public class BaseLubanConfig implements ILubanConfig {
    @Override
    public void reportException(Throwable throwable) {
        if(LubanUtil.enableLog){
            throwable.printStackTrace();
        }
    }

    @Override
    public File getSaveDir() {
        return LubanUtil.app.getExternalFilesDir(Environment.DIRECTORY_DCIM);
    }

    @Override
    public void trace(long timeCost, int percent, long sizeAfterCompressInK, long width, long height) {
        LubanUtil.i("time cost : "+timeCost+"ms, filesize after compress:"+sizeAfterCompressInK +"kB , 压缩比:"+percent +"%,  wh:"+width+"x"+height);
    }

}
