package top.zibin.luban.example;

import android.graphics.Bitmap;

import com.hss01248.lubanturbo.TurboCompressor;

import java.io.File;
import java.io.IOException;

import top.zibin.luban.Engine;
import top.zibin.luban.IBitmapToFile;
import top.zibin.luban.Luban;

public class LubanTurbo implements IBitmapToFile {
    @Override
    public File compressToFile(Bitmap tagBitmap, File tagImg, boolean focusAlpha, int quality, Luban luban, Engine engine) throws IOException {
        boolean success =  TurboCompressor.compressToFile(tagBitmap,tagImg,quality);
        if(success){
            return tagImg;
        }
        throw  new IOException("compressed by turbo failed");
    }
}
