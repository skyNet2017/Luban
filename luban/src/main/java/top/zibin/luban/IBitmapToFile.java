package top.zibin.luban;

import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;

/**
 * Created by hss on 2018/12/14.
 */

public interface IBitmapToFile {

    File compressToFile(Bitmap tagBitmap, File tagImg, boolean focusAlpha, int quality, Luban luban) throws IOException;
}
