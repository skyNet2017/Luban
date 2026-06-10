package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;

import androidx.exifinterface.media.ExifInterface;

import com.blankj.utilcode.util.LogUtils;
import com.hss01248.media.metadata.ExifUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

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


    private Bitmap transformImage(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(270);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }
        Bitmap result = null;
        try {
            result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            LubanUtil.config.reportException(e);
        }
        if (result == null) {
            return bitmap;
        }
        if (result != bitmap) {
            bitmap.recycle();
        }
        return result;
    }

    File compress() {

        try {
            //先使用双线性采样,oom了再使用单线性采样,还oom就强制压缩到720p, 但最后还是可能抛出oom
            Bitmap tagBitmap = decodeToBitmap();

            //it.sephiroth.android.library.exif2.ExifInterface exifInterface = null;
            Map<String, String> exifs = null;
            int rotation = 0;
            FileInputStream exifFis = null;
            try {
                exifFis = new FileInputStream(srcImg.getPath());
                exifs = ExifUtil.readExif(exifFis);
            } catch (Throwable throwable) {
                LubanUtil.config.reportException(throwable);
            } finally {
                LubanUtil.closeIO(exifFis);
            }
            boolean rotateSuccess = false;

            //webp也有exif
            if (exifs != null) {
                String ori = exifs.get(ExifInterface.TAG_ORIENTATION);
                if (!TextUtils.isEmpty(ori)) {
                    try {
                        int o = Integer.parseInt(ori);
                        if (o != ExifInterface.ORIENTATION_NORMAL && o != ExifInterface.ORIENTATION_UNDEFINED) {
                            rotation = o;
                            Bitmap transformed = transformImage(tagBitmap, o);
                            rotateSuccess = (transformed != tagBitmap);
                            tagBitmap = transformed;
                        }
                    } catch (Throwable throwable) {
                        LubanUtil.config.reportException(throwable);
                    }
                }
            }
            bitmapToFile.compressToJpg(tagBitmap, tagImg, focusAlpha, quality, luban, this);

            //todo 压缩后比源文件还大? 是要压缩的文件还是源文件?

            //todo 限制大小

            if (exifs != null && !luban.toAvif) {
                try{
                    if (luban.keepExif) {
                        ExifUtil.resetImageWHToMap(exifs, new FileInputStream(new File(tagImg.getAbsolutePath())), rotateSuccess);
                        if (rotateSuccess) {
                            exifs.put(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                        }
                        ExifUtil.writeExif(exifs, tagImg.getAbsolutePath());
                    } else {
                        try {
                            ExifInterface exif = new ExifInterface(tagImg);
                            if (rotateSuccess) {
                                exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                                exif.saveAttributes();
                            } else if (rotation != 0) {
                                exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(rotation));
                                exif.saveAttributes();
                            }
                        } catch (Throwable throwable) {
                            LubanUtil.config.reportException(throwable);
                        }
                    }
                }catch (Throwable throwable){
                    LogUtils.w(throwable);
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
    private Bitmap decodeToBitmap() {

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
            //使用双线性插值  filter=true
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getPath(),options);
            if (scale != 1f) {
                tagBitmap2 = Bitmap.createScaledBitmap(tagBitmap, (int) (srcWidth / scale), (int) (srcHeight / scale), true);
                if (tagBitmap2 != tagBitmap) {
                    tagBitmap.recycle();
                }
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