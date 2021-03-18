package top.zibin.luban.example;

import android.app.Application;

import androidx.multidex.MultiDexApplication;

import com.blankj.utilcode.util.Utils;

import top.zibin.luban.LubanUtil;

public class BaseApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        LubanUtil.init(this,true,null);
        Utils.init(this);
    }
}
