package com.hss01248.lubanturbo;

import android.graphics.Bitmap;
import android.util.Log;


import java.io.File;


/**
 * Created by hss on 2018/12/14.
 */

public class TurboCompressor {

    static {
        try {
            System.loadLibrary("luban");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

    public native static boolean nativeCompress(Bitmap bitmap, int quality, boolean isRGB565, String outPath);


    public static boolean compressToFile(Bitmap tagBitmap, File tagImg, int quality) {
        boolean isSuccess = false;
        long start = System.currentTimeMillis();
        try {
            isSuccess = nativeCompress(tagBitmap, quality, tagBitmap.getConfig() == Bitmap.Config.RGB_565, tagImg.getAbsolutePath());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        Log.d("dd", "TurboCompressor ended,cost time:" + (System.currentTimeMillis() - start));

        return isSuccess;
    }

}
