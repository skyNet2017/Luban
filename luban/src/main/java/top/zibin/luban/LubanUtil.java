package top.zibin.luban;

import android.app.Application;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public class LubanUtil {

    private static final int MAX_IMAGE_WIDTH = 720;

    private static final int MAX_IMAGE_HEIGHT = 720;

    private static final long MIN_IMAGE_COMPRESS_SIZE = 150 * 1024;//150k以下,不压缩
    
    static Application app;
    private static ILubanConfig config;
     static boolean enableLog;

    public static void init(Application app,boolean enableLog,@Nullable  ILubanConfig config){
        LubanUtil.app = app;
        LubanUtil.enableLog = enableLog;
        LubanUtil.config = config;
        if(config == null){
            LubanUtil.config = new BaseLubanConfig();
        }
    }


    public static File compressByLuban(String imgPath, boolean isPng) {
        File file0 = null;
        try {
            file0 = new File(imgPath);
            if (file0.length() <= MIN_IMAGE_COMPRESS_SIZE) {
                return file0;
            }
            logFile("compressByLuban begin",imgPath);
            long start = System.currentTimeMillis();
            File file = Luban.with(app)
                    .ignoreBy(150)
                    .setTargetDir(config.getSaveDir().getAbsolutePath())
                    .setFocusAlpha(isPng)
                    .get(imgPath);
            i("compressByLuban cost " + (System.currentTimeMillis() - start) + " ms");
            logFile("compressByLuban end",file.getAbsolutePath());
            if (file.exists()) {
                return file;
            }
            return new File(imgPath);
        } catch (OutOfMemoryError e) {
            config.reportException(e);
            return compressBySubsamling2(imgPath);
        } catch (Throwable e) {
            config.reportException(e);
            return new File(imgPath);
        }
    }

    public static void compressByLubanAsync(final String imgPath, boolean isPng,
                                            final CompressCallback callback) {
        final File file = new File(imgPath);
        if (!file.exists()) {
            callback.onError(new Throwable("file not exist"));
            return;
        }
        if (file.length() <= MIN_IMAGE_COMPRESS_SIZE) {
            callback.onSuccess(file);
            return;
        }
        logFile("compressByLubanAsync begin",imgPath);
        Luban.with(app)
                .setTargetDir(config.getSaveDir().getAbsolutePath())
                .setFocusAlpha(isPng)
                .ignoreBy(150)
                .load(file)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        logFile("compressByLubanAsync success",imgPath);
                        callback.onSuccess(file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        config.reportException(e);
                        if (file.exists()) {
                            if (e instanceof OutOfMemoryError) {
                                config.reportException(e);
                                File file1 =  compressBySubsamling2(imgPath);
                                callback.onSuccess(file1);
                                return;
                            }
                            callback.onSuccess(file);
                        } else {
                            callback.onError(e);
                        }

                    }
                }).launch();

    }

    private static void i(String s) {
        if(enableLog && !TextUtils.isEmpty(s)){
            Log.i("lubanutil",s);
        }
    }

    private static String formatFileSize(long size) {
        try {
            DecimalFormat dff = new DecimalFormat(".00");
            if (size >= 1024 * 1024) {
                double doubleValue = ((double) size) / (1024 * 1024);
                String value = dff.format(doubleValue);
                return value + "MB";
            } else if (size > 1024) {
                double doubleValue = ((double) size) / 1024;
                String value = dff.format(doubleValue);
                return value + "KB";
            } else {
                return size + "B";
            }
        } catch (Exception e) {
            config.reportException(e);
        }
        return String.valueOf(size);
    }

    public interface CompressCallback{
        void onSuccess(File file);

        void onError(Throwable e);
    }




    private static File compressBySubsamling2(String imgPath) {
        logFile("compressBySubsamling2 begin",imgPath);
        Bitmap bitmap = decodeRGB565BitmapFromUri(Uri.fromFile(new File(imgPath)), MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
        String fileName = new File(config.getSaveDir(),System.currentTimeMillis() + "-compressed2.jpg").getAbsolutePath();

        boolean success = saveBitmapToFile(fileName, bitmap);
        logFile("compressBySubsamling2 result",fileName);
        if (success) {
            return new File(fileName);
        }
        return new File(imgPath);
    }

   static void  logFile(String desc,String file){
        if(enableLog){
            i(desc+" :  size:"+ formatFileSize(new File(file).length()) +", "+readExif(file));
        }

    }

    /**
     * 保存图片到文件
     */
    private static boolean saveBitmapToFile(String filename, Bitmap bitmap) {
        return saveBitmapToFile(filename, bitmap, Bitmap.CompressFormat.JPEG);
    }



    private static boolean saveBitmapToFile(String filename, Bitmap bitmap, Bitmap.CompressFormat compressFormat) {
        return saveBitmapToFile(filename, bitmap, 80, compressFormat);
    }

    /**
     * 保存图片到文件
     */
    private static boolean saveBitmapToFile(String filename, Bitmap bitmap, int quality,
                                            Bitmap.CompressFormat compressFormat) {
        if (null == bitmap || TextUtils.isEmpty(filename)) {
            return false;
        }
        FileOutputStream fos = null;
        try {
            File file = new File(filename);
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            fos = new FileOutputStream(file);
            bitmap.compress(compressFormat, quality, fos);
            fos.flush();

            return true;
        } catch (Exception e) {
            config.reportException(e);
            return false;
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                config.reportException(e);
            }
        }
    }

    private static Bitmap decodeRGB565BitmapFromUri(Uri imageUri, int width, int height) {
        return decodeBitmapFromUri(imageUri, width, height, Bitmap.Config.RGB_565);
    }



    private static Bitmap decodeBitmapFromUri(Uri imageUri, int width, int height,
                                              Bitmap.Config config) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            String scheme = imageUri.getScheme();
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)) {
                ContentResolver resolver = app.getContentResolver();
                InputStream imageStream = resolver.openInputStream(imageUri);//读文件会有bug
                BitmapFactory.decodeStream(imageStream, null, options);
                imageStream.close();
            } else {
                BitmapFactory.decodeFile(imageUri.toString(), options);
            }
            if (width > 0 && height > 0) {
                if (options.outWidth > width || options.outHeight > height) {
                    options.inSampleSize = Math.max(options.outWidth / width, options.outHeight / height);
                }
            }
            i( "options.inSampleSize is:" + options.inSampleSize);
            if (options.inSampleSize < 1) {
                options.inSampleSize = 1;
            }
            options.inPreferredConfig = config;
            options.inJustDecodeBounds = false;
            Bitmap bmp = null;
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)) {
                ContentResolver resolver = app.getContentResolver();
                InputStream imageStream = resolver.openInputStream(imageUri);
               /* if(ContentResolver.SCHEME_FILE.equals(scheme)){
                    imageStream = new FileInputStream(imageUri.getPath());
                }else {
                    imageStream = resolver.openInputStream(imageUri);
                }*/

                int degree = getBitmapDegree(imageStream);
                imageStream = resolver.openInputStream(imageUri);
                bmp = BitmapFactory.decodeStream(imageStream, null, options);
                if (degree % 360 != 0) {
                    bmp = rotateBitmapByDegree(bmp, degree);
                }
                if (imageStream != null) {
                    imageStream.close();
                }
            } else {
                bmp = BitmapFactory.decodeFile(imageUri.toString(), options);
                int degree = getBitmapDegree(imageUri.getPath());
                if (degree % 360 != 0) {
                    bmp = rotateBitmapByDegree(bmp, degree);
                }
            }
            return bmp;
        } catch (Exception e) {
            LubanUtil.config.reportException(e);
            return null;
        }
    }

    private static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        return rotateBitmapByDegree(bm, degree, true);
    }

    private static Bitmap rotateBitmapByDegree(Bitmap bm, int degree, boolean recycle) {
        if (bm == null || bm.isRecycled()) {
            return null;
        }

        if (degree == 0) {
            return bm;
        }

        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            config.reportException(e);
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm && recycle) {
            bm.recycle();
        }
        return returnBm;
    }
    /**
     * 读取图片的旋转的角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    static int getBitmapDegree(String path) {
        if (TextUtils.isEmpty(path)) {
            return 0;
        }

        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            config.reportException(e);
        }
        return degree;
    }

    private static int getBitmapDegree(InputStream inputStream) {
        if (inputStream == null) {
            return 0;
        }

        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(inputStream);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            config.reportException(e);
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return degree;
    }

   public   static String readExif(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }

            List<String> tags = getTags();
            it.sephiroth.android.library.exif2.ExifInterface exifInterface1 = new it.sephiroth.android.library.exif2.ExifInterface();
            exifInterface1.readExif(path, it.sephiroth.android.library.exif2.ExifInterface.Options.OPTION_ALL);
            int quality = exifInterface1.getQualityGuess();
            int[] wh = getImageWidthHeight(path);

            StringBuilder sb = new StringBuilder();
            sb.append("file info:");
            sb.append("\n")
                    .append(path)
                    .append("\nwh")
                    .append(wh[0])
                    .append("x")
                    .append(wh[1])
                    .append("\nsize:")
                    .append(formatFileSize(new File(path).length()))
                    .append("\ntype:")
                    .append(getRealType(new File(path)));
            sb.append("\njpeg quality guess:").append(quality);
            sb.append("\norientation degree:").append(degree);
            for (String tag: tags) {
                String attr = exifInterface.getAttribute(tag);
                if(!TextUtils.isEmpty(attr)){
                    sb.append("\n").append(tag)
                            .append(":")
                            .append(attr);
                }
            }
            return sb.toString();

        } catch (Throwable e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }

    private static String getRealType(File file) {
        if(!file.exists()){
            return "";
        }
        if(file.getName().endsWith(".gif")){
            return "gif";
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] b = new byte[4];
            try {
                is.read(b, 0, b.length);
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
            String type = bytesToHexString(b).toUpperCase();
            if (type.contains("FFD8FF")) {
                return "jpg";
            } else if (type.contains("89504E47")) {
                return "png";
            } else if (type.contains("47494638")) {
                return "gif";
            } else if (type.contains("49492A00")) {
                return "tif";
            } else if (type.contains("424D")) {
                return "bmp";
            }
            return type;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            try {
                if(is != null){
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static int[] getImageWidthHeight(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        /**
         * 最关键在此，把options.inJustDecodeBounds = true;
         * 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        options.inJustDecodeBounds = true;
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path, options); // 此时返回的bitmap为null
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         *options.outHeight为原始图片的高
         */
        return new int[]{options.outWidth, options.outHeight};
    }

    static List<String> tags;

    private static List<String> getTags(){
        if(tags != null && !tags.isEmpty()){
            return tags;
        }
        tags = new ArrayList<>();
        Class clazz = android.media.ExifInterface.class;
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().startsWith("TAG_")){
                try {
                    field.setAccessible(true);
                    tags.add(field.get(null).toString());
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
        }
        return tags;
    }
}
