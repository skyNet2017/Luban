package top.zibin.luban;


import java.io.File;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public class BaseLubanConfig implements ILubanConfig {
    @Override
    public void reportException(Throwable throwable) {
        if (LubanUtil.enableLog) {
            throwable.printStackTrace();
        }
    }

    @Override
    public File getSaveDir() {
        File dir = LubanUtil.app.getFilesDir();
        File subDir = new File(dir, "luban");
        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        return subDir;
    }


}
