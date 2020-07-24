package top.zibin.luban.example;

import android.app.Application;

import top.zibin.luban.ILubanConfig;
import top.zibin.luban.LubanUtil;

public class BaseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LubanUtil.init(this, true, new ILubanConfig() {
            @Override
            public boolean keepExif() {
                return true;
            }
        });
    }
}
