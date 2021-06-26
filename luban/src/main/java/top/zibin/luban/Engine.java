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
public class Engine {
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

    Engine(InputStreamProvider srcImg, File tagImg, boolean focusAlpha, IBitmapToFile bitmapToFile, int quality, Luban luban) {
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
        this.originalMimeType = options.outMimeType;
        LubanUtil.d("类型:" + originalMimeType);

    }


    private Bitmap rotatingImage(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    File compress() {

        try {
            //先使用双线性采样,oom了再使用单线性采样,还oom就强制压缩到720p, 但最后还是可能抛出oom
            Bitmap tagBitmap = compressBitmap();

            //it.sephiroth.android.library.exif2.ExifInterface exifInterface = null;
            Map<String, String> exifs = null;
            int rotation = 0;
            try {
                //是否能读webp的exif?
                //还是用原生的api吧,兼容性好点?
                //原生在Android10后 读经纬度还有定位权限要求,否则崩溃.
                exifs = ExifUtil.readExif(new FileInputStream(srcImg.getPath()));
                //exifInterface = new it.sephiroth.android.library.exif2.ExifInterface();
                // exifInterface.readExif(srcImg.getPath(), it.sephiroth.android.library.exif2.ExifInterface.Options.OPTION_ALL);
            } catch (Throwable throwable) {
                LubanUtil.config.reportException(throwable);
            }
            boolean rotateSuccess = false;

            //webp也有exif
            if (exifs != null) {
                String ori = exifs.get("Orientation");
                if (TextUtils.isEmpty(ori)) {
                    try {
                        int o = Integer.parseInt(ori);
                        if (o != 0) {
                            rotation = o;
                            //可能oom. 万一oom了,图片还是留着,但是exif丽保留原旋转角度.
                            tagBitmap = rotatingImage(tagBitmap, o);
                            rotateSuccess = true;
                        }
                    } catch (Throwable throwable) {
                        LubanUtil.config.reportException(throwable);
                    }
                }
            }
            bitmapToFile.compressToFile(tagBitmap, tagImg, focusAlpha, quality, luban, this);

            if (exifs != null) {
                if (luban.keepExif) {
                    //最后一个参数代表是否要复写Orientation参数为0. 旋转成功就复写,没有成功就维持原先的
                    ExifUtil.resetImageWHToMap(exifs, new FileInputStream(new File(tagImg.getAbsolutePath())), rotateSuccess);
                    ExifUtil.writeExif(exifs, tagImg.getAbsolutePath());
                } else {
                    if (!rotateSuccess && rotation != 0) {
                        //rotation回写:
                        try {
                            ExifInterface exif = new ExifInterface(tagImg);
                            exif.setAttribute("Orientation", rotation + "");
                            exif.saveAttributes();
                        } catch (Throwable throwable) {
                            LubanUtil.config.reportException(throwable);
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            if (LubanUtil.config != null) {
                LubanUtil.config.reportException(throwable);
            }
            //还TMD不行,老子不压了,返回原图
            tagImg = new File(srcImg.getPath());
        }
        return tagImg;
    }


    //先使用双线性采样,oom了再使用单线性采样,还oom就强制压缩到720p
    private Bitmap compressBitmap() {

        float scale = 1f;
        if (!luban.noResize) {
            if (luban.maxShortDimension != 0) {
                //指定压缩上限:
                int shorter = Math.min(srcHeight, srcWidth);
                if (shorter > luban.maxShortDimension) {
                    scale = shorter * 1f / luban.maxShortDimension;
                }
            } else {
                //Luban.computeInSampleSize下限1080p
                scale = Luban.computeInSampleSize(srcWidth, srcHeight);
            }
        }


        //获取原图的类型
        //String mimeType = options.outMimeType;
        //如果是png,看是否有透明的alpha通道,如果没有,给你压成jpg. 如果有,用白色填充.

        Bitmap tagBitmap2 = null;

        //计算个毛线,直接申请内存,oom了就降级:
        //压缩插值算法效果见: https://cloud.tencent.com/developer/article/1006352
        try {
            //使用双线性插值
            Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getPath());
            if (scale != 1f) {
                tagBitmap2 = Bitmap.createScaledBitmap(tagBitmap, (int) (srcWidth / scale), (int) (srcHeight / scale), true);
            } else {
                tagBitmap2 = tagBitmap;
            }

        } catch (OutOfMemoryError throwable) {
            LubanUtil.config.reportException(throwable);
            try {
                //使用单线性插值
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = (int) scale;
                //优先使用888. 因为RGB565在低版本手机上会变绿
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
            } catch (OutOfMemoryError throwable1) {
                LubanUtil.config.reportException(throwable1);

                //用RGB_565, 如果原图是png,且有透明的alpha通道,那么会变黑. 如何处理?
                try {
                    //使用RGB565将就一下:
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = (int) scale;
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
                    isPngWithTransAlpha = false;
                } catch (OutOfMemoryError error) {
                    LubanUtil.config.reportException(error);
                    //try {
                    //还TMD不行,只能压一把狠的:强制压缩到720p:
                    int w = Math.min(srcHeight, srcWidth);
                    scale = w / 720f;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = (int) scale;
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