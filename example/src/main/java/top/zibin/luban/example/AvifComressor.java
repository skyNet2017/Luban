package top.zibin.luban.example;

import android.graphics.Bitmap;

import com.radzivon.bartoshyk.avif.coder.AvifSpeed;
import com.radzivon.bartoshyk.avif.coder.HeifCoder;
import com.radzivon.bartoshyk.avif.coder.PreciseMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import top.zibin.luban.Engine;
import top.zibin.luban.IBitmapToFile;
import top.zibin.luban.Luban;

/**
 * @Despciption todo
 * @Author hss
 * @Date 7/2/24 3:57 PM
 * @Version 1.0
 */
public class AvifComressor  implements IBitmapToFile {
    @Override
    public File compressToJpg(Bitmap tagBitmap, File tagImg, boolean focusAlpha,
                              int quality, Luban luban, Engine engine) throws IOException {


        byte[] bytes = new HeifCoder().encodeAvif(tagBitmap,quality, PreciseMode.LOSSY, AvifSpeed.FIVE);
        FileOutputStream fileOutputStream = new FileOutputStream(tagImg);
        fileOutputStream.write(bytes);
        fileOutputStream.flush();
        fileOutputStream.close();
        return tagImg;
    }
}
