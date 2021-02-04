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
    public File compressToFile(Bitmap tagBitmap, File tagImg, boolean focusAlpha, int quality, Luban luban) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if(luban.targetFormat.equals(Bitmap.CompressFormat.WEBP)){
            //https://developer.android.com/reference/android/graphics/Bitmap.CompressFormat
            //Compress to the WEBP format. quality of 0 means compress for the smallest size.
            // 100 means compress for max visual quality. As of Build.VERSION_CODES.Q,
            // a value of 100 results in a file in the lossless WEBP format.
            // Otherwise the file will be in the lossy WEBP format.
            //100的会比99大很多,非常不划算.
            //webp可用于算法模型分析.因为jpg格式压缩会损失非常多的精度,极大影响图像相似度比对的结果
            quality = 99;
        }
        tagBitmap.compress(luban.targetFormat, quality, stream);//focusAlpha ? Bitmap.CompressFormat.PNG :
        tagBitmap.recycle();

        FileOutputStream fos = new FileOutputStream(tagImg);
        fos.write(stream.toByteArray());
        fos.flush();
        try {
            fos.close();
            stream.close();
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }

        return tagImg;
    }
}
