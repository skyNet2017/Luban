package top.zibin.luban;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hss on 2018/12/14.
 */

public class DefaultBitmapToFile implements IBitmapToFile {
    @Override
    public void compressToFile(Bitmap tagBitmap, File tagImg, boolean focusAlpha, int quality) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        tagBitmap.compress(focusAlpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, quality, stream);
        tagBitmap.recycle();

        FileOutputStream fos = new FileOutputStream(tagImg);
        fos.write(stream.toByteArray());
        fos.flush();
        fos.close();
        stream.close();
    }
}
