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
}
