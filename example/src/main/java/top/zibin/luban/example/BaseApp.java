package top.zibin.luban.example;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.multidex.MultiDexApplication;

import com.blankj.utilcode.util.Utils;
import com.hss01248.luban.showresult.ShowResultUtil;

import top.zibin.luban.BaseLubanConfig;
import top.zibin.luban.LubanUtil;

public class BaseApp extends MultiDexApplication {

    static Activity top;
    @Override
    public void onCreate() {
        super.onCreate();
        ShowResultUtil.showCompressResult = false;
        LubanUtil.init(this,true,new BaseLubanConfig(){
            @Override
            public void trace(String inputPath, String outputPath, long timeCost, int percent, long sizeAfterCompressInK, long width, long height) {
                ShowResultUtil.showResult(top,inputPath,outputPath,timeCost,percent,sizeAfterCompressInK,width,height);
            }

            @Override
            public boolean editExif(@Nullable ExifInterface exif) {
                if(exif == null){
                    return true;
                }
                exif.setAttribute(ExifInterface.TAG_ARTIST,"sb");
                return true;
            }
        });
        Utils.init(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                top = activity;
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                top = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
    }
}
