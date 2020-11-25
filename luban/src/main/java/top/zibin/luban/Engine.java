package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.sax.TemplatesHandler;

import it.sephiroth.android.library.exif2.ExifInterface;

/**
 * Responsible for starting compress and managing active and cached resources.
 */
class Engine {
  private InputStreamProvider srcImg;
  private File tagImg;
  private int srcWidth;
  private int srcHeight;
  private boolean focusAlpha;
  IBitmapToFile bitmapToFile;
  private int quality;
  Luban luban;

  Engine(InputStreamProvider srcImg, File tagImg, boolean focusAlpha,IBitmapToFile bitmapToFile,int quality,Luban luban) throws IOException {
    this.tagImg = tagImg;
    this.srcImg = srcImg;
    this.focusAlpha = focusAlpha;
    this.bitmapToFile = bitmapToFile;
    this.quality = quality;
    this.luban = luban;

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 1;

    BitmapFactory.decodeFile(srcImg.getPath(), options);
    this.srcWidth = options.outWidth;
    this.srcHeight = options.outHeight;
  }

  private Bitmap rotatingImage(Bitmap bitmap, int angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
  }

  File compress() throws IOException {
    //先预判
    //先使用双线性采样,oom了再使用单线性采样,还oom就强制压缩到1080p


    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = Luban.computeInSampleSize(srcWidth,srcHeight);//单线性采样
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;//避免oom,解压这一步就会变绿
    Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getPath(), options);








    ExifInterface exifInterface = null;
    if (Checker.SINGLE.isJPG(srcImg.getPath())) {
      int oritation = Checker.SINGLE.getOrientation(srcImg.getPath());
      if(oritation != 0){
        tagBitmap = rotatingImage(tagBitmap, oritation);
      }
      if(luban.keepExif){
        try {
          exifInterface = new ExifInterface();
          exifInterface.readExif(srcImg.getPath(),ExifInterface.Options.OPTION_ALL);
        }catch (Throwable throwable){
          throwable.printStackTrace();
        }
      }
    }
    bitmapToFile.compressToFile(tagBitmap,tagImg,focusAlpha,quality,luban);
    if(exifInterface!= null){
      exifInterface.writeExif(tagImg.getAbsolutePath());
    }
    return tagImg;
  }


}