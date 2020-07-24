package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
  Luban luban;

  Engine(InputStreamProvider srcImg, File tagImg, Luban luban) throws IOException {
    this.tagImg = tagImg;
    this.srcImg = srcImg;
    this.focusAlpha = luban.focusAlpha;
    this.bitmapToFile = luban.bitmapToFile;
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
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = Luban.computeInSampleSize(srcWidth,srcHeight);
    if(luban.useRGB888){
      options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }else {
      options.inPreferredConfig = Bitmap.Config.RGB_565;//避免oom. 可能会导致图片变绿
    }
    Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getPath(), options);
    Map<String,String> exif = new HashMap<>();
    if(luban.keepExif){
      //拷贝exif
       exif = ExifUtil.readExif(new FileInputStream(srcImg.getPath()));
    }

    if (Checker.SINGLE.isJPG(srcImg.getPath())) {
      int oritation = Checker.SINGLE.getOrientation(srcImg.getPath());
      if(oritation != 0){
        tagBitmap = rotatingImage(tagBitmap, oritation);
      }
    }
    bitmapToFile.compressToFile(tagBitmap,tagImg,focusAlpha,Luban.TARGET_QUALITY);
    if(luban.keepExif){
      ExifUtil.writeExif(exif,tagImg.getAbsolutePath());
    }

    return tagImg;
  }


}