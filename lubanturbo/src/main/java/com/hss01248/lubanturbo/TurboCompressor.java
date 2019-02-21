package com.hss01248.lubanturbo;

import android.graphics.Bitmap;
import android.util.Log;
import top.zibin.luban.IBitmapToFile;

import java.io.File;
import java.io.IOException;

/**
 * Created by hss on 2018/12/14.
 */

public class TurboCompressor {

    static {
        System.loadLibrary("luban");
    }

    public native static boolean nativeCompress(Bitmap bitmap,int quality,boolean isRGB565,String outPath);


    public static IBitmapToFile getTurboCompressor(){
        return new IBitmapToFile() {
            @Override
            public void compressToFile(Bitmap tagBitmap, File tagImg, boolean focusAlpha, int quality) throws IOException {
                Log.d("dd","TurboCompressor started  tagBitmap.getConfig():"+tagBitmap.getConfig());
                long start = System.currentTimeMillis();
                boolean isSuccess = nativeCompress(tagBitmap,quality,tagBitmap.getConfig() == Bitmap.Config.RGB_565,tagImg.getAbsolutePath());
                Log.d("dd","TurboCompressor ended,cost time:"+(System.currentTimeMillis() - start));
                if(!isSuccess){
                    throw new IOException("nativeCompress failed");
                }
            }
        };
    }
}
