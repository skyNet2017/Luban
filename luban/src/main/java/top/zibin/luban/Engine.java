package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.sax.TemplatesHandler;

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

    try {
      //先使用双线性采样,oom了再使用单线性采样,还oom就强制压缩到720p, 但最后还是可能抛出oom
      Bitmap tagBitmap = compressBitmap();
      it.sephiroth.android.library.exif2.ExifInterface exifInterface = null;
      //webp也有exif
      if (Checker.SINGLE.isJPG(srcImg.getPath())) {
        int oritation = Checker.SINGLE.getOrientation(srcImg.getPath());
        if(oritation != 0){
          tagBitmap = rotatingImage(tagBitmap, oritation);
        }
        if(luban.keepExif){
          //是否能读webp的exif?
          //还是用原生的api吧,兼容性好点?
          //原生在Android10后 读经纬度还有定位权限要求,否则崩溃.
          try {
            exifInterface = new it.sephiroth.android.library.exif2.ExifInterface();
            exifInterface.readExif(srcImg.getPath(), it.sephiroth.android.library.exif2.ExifInterface.Options.OPTION_ALL);
          }catch (Throwable throwable){
            throwable.printStackTrace();
          }
        }
      }
      bitmapToFile.compressToFile(tagBitmap,tagImg,focusAlpha,quality,luban);
      if(exifInterface!= null){
        exifInterface.writeExif(tagImg.getAbsolutePath());
      }
    }catch (Throwable throwable){
      if(LubanUtil.config != null){
        LubanUtil.config.reportException(throwable);
      }

      //还TMD不行,老子不压了,返回原图
      tagImg = new File(srcImg.getPath());
    }
    return tagImg;
  }



  //先使用双线性采样,oom了再使用单线性采样,还oom就强制压缩到720p
  private Bitmap compressBitmap() {

    //下限1080p
    int scale = Luban.computeInSampleSize(srcWidth,srcHeight);
    Bitmap tagBitmap2 = null;

    //计算个毛线,直接申请内存,oom了就降级:
    //压缩插值算法效果见: https://cloud.tencent.com/developer/article/1006352
    try {
      //使用双线性插值
      Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getPath());
      tagBitmap2 = Bitmap.createScaledBitmap(tagBitmap,srcWidth/scale,srcHeight/scale,true);
      //tagBitmap.recycle();
    }catch (OutOfMemoryError throwable){
      throwable.printStackTrace();

      try {
        //使用单线性插值
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = scale;
        //优先使用888. 因为RGB565在低版本手机上会变绿
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
      }catch (OutOfMemoryError throwable1){
        throwable1.printStackTrace();

        try {
          //使用RGB565将就一下:
          BitmapFactory.Options options = new BitmapFactory.Options();
          options.inSampleSize = scale;
          options.inPreferredConfig = Bitmap.Config.RGB_565;
          tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
        }catch (OutOfMemoryError error){
          error.printStackTrace();
          //try {
            //还TMD不行,只能压一把狠的:强制压缩到720p:
            int w = Math.min(srcHeight,srcWidth);
            scale =  w/720;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
          //}catch (OutOfMemoryError error2){
          //  error2.printStackTrace();
            //还TMD不行,老子不压了,返回原图: 在外面处理:
         // }
        }
      }
    }
    return tagBitmap2;
  }


}