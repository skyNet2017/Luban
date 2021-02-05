package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.hss01248.media.metadata.ExifUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

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
   int quality;
   String originalMimeType;
   boolean isPngWithTransAlpha;
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
    Log.d("luban","original path:"+srcImg.getPath());
    BitmapFactory.decodeFile(srcImg.getPath(), options);
    this.srcWidth = options.outWidth;
    this.srcHeight = options.outHeight;
    this.originalMimeType = options.outMimeType;
    Log.d("luban","类型:"+originalMimeType);
    calAlpha();
  }

  /**
   * 计算图像内是否有透明的像素点
   */
  private void calAlpha() {
    if(!"image/png".equals(originalMimeType)){
      return ;
    }
    try {
      long start = System.currentTimeMillis();
      //最长边压缩到360p,看像素点内是否有不透明的像素点
      int max = Math.max(srcHeight,srcWidth);
      int scale = 1;
      if(max > 50){
        //限定长边100时,最大耗时400ms
        //限定50时,最大耗时82ms
        //最快: 四个角判断是否为0, 1ms即可.
        scale = (int) Math.ceil(max/50f);
      }
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = scale;
      //优先使用888. 因为RGB565在低版本手机上会变绿
      options.inPreferredConfig = Bitmap.Config.ARGB_8888;
      Bitmap bitmap = BitmapFactory.decodeFile(srcImg.getPath(), options);

      int w = srcWidth/scale;
      int h = srcHeight/scale;
      //可以先判断4个角
     out: for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
          // The argb {@link Color} at the specified coordinate
          int pix = bitmap.getPixel(i,j);
          int a = ((pix >> 24) & 0xff) ;/// 255.0f
          //Log.d("luban","位置:"+i+"-"+j+", 不透明度:"+a);
          //255就是没有透明度, = 0 就是完全透明. 值代表不透明度.值越大,越不透明
          if(a != 255 ){
            isPngWithTransAlpha = true;
            break out;
          }
          //FF: 255   00 : 0
          //Color color = Color.valueOf(pix);
          //color.alpha()
        }
      }
      Log.d("luban","cal alpah cost(ms):"+(System.currentTimeMillis() - start));
    }catch (Throwable throwable){
      throwable.printStackTrace();
    }
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
      //it.sephiroth.android.library.exif2.ExifInterface exifInterface = null;
      Map<String,String> exifs = null;

      try {
        //是否能读webp的exif?
        //还是用原生的api吧,兼容性好点?
        //原生在Android10后 读经纬度还有定位权限要求,否则崩溃.
        exifs = ExifUtil.readExif(new FileInputStream(srcImg.getPath()));
        //exifInterface = new it.sephiroth.android.library.exif2.ExifInterface();
        // exifInterface.readExif(srcImg.getPath(), it.sephiroth.android.library.exif2.ExifInterface.Options.OPTION_ALL);
      }catch (Throwable throwable){
        throwable.printStackTrace();
      }
      //webp也有exif
      if (exifs != null ) {
        String ori = exifs.get("Orientation");
        if(TextUtils.isEmpty(ori)){
          try {
           int o =  Integer.parseInt(ori);
            tagBitmap = rotatingImage(tagBitmap, o);
          }catch (Throwable throwable){
            throwable.printStackTrace();
          }
        }
      }
      bitmapToFile.compressToFile(tagBitmap,tagImg,focusAlpha,quality,luban,this);
      /*if(exifInterface!= null){
        exifInterface.writeExif(tagImg.getAbsolutePath());
      }*/
      if(exifs != null && luban.keepExif){
        ExifUtil.resetImageWHToMap(exifs,tagImg.getAbsolutePath(),true);
        ExifUtil.writeExif(exifs,tagImg.getAbsolutePath());
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
    //Luban.computeInSampleSize下限1080p
    int scale = Luban.computeInSampleSize(srcWidth,srcHeight);
    //获取原图的类型
    //String mimeType = options.outMimeType;
    //如果是png,看是否有透明的alpha通道,如果没有,给你压成jpg. 如果有,用白色填充.

    Bitmap tagBitmap2 = null;

    //计算个毛线,直接申请内存,oom了就降级:
    //压缩插值算法效果见: https://cloud.tencent.com/developer/article/1006352
    try {
      //使用双线性插值
      Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getPath());
      tagBitmap2 = Bitmap.createScaledBitmap(tagBitmap,srcWidth/scale,srcHeight/scale,true);
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

        //用RGB_565, 如果原图是png,且有透明的alpha通道,那么会变黑. 如何处理?
        try {
          //使用RGB565将就一下:
          BitmapFactory.Options options = new BitmapFactory.Options();
          options.inSampleSize = scale;
          options.inPreferredConfig = Bitmap.Config.RGB_565;
          tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
          isPngWithTransAlpha = false;
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
            isPngWithTransAlpha = false;
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