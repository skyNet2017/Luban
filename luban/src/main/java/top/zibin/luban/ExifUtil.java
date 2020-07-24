package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class ExifUtil {



    public static Map<String,String> readExif(InputStream inputStream){
       return readExif(inputStream,true);
    }
    public static Map<String,String> readExif(InputStream inputStream,boolean close){
        Map<String,String> exifMap = new HashMap<>();
        try {
            ExifInterface exif = new ExifInterface(inputStream);
            Class exifClazz = ExifInterface.class;
            Field[] fields = exifClazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if(field.getName().startsWith("TAG_")){
                    field.setAccessible(true);
                    try {
                        String tag = (String) field.get(ExifInterface.class);
                        String val = exif.getAttribute(tag);
                        exifMap.put(tag,val);

                    }catch (Throwable throwable){
                        //log(throwable);
                    }
                }
            }
        } catch (Exception e) {
           log(e);
        }finally {
            if(close){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log(exifMap);
            return exifMap;
        }
    }

    public static void writeExif(Map<String,String> exifMap, String file){
        try{
            if(exifMap.isEmpty()){
              log("exifMap.isEmpty");
                return;
            }
            File file1 = new File(file);
            if(!file1.exists()){
                log("file not exist:"+file);
                return;
            }
            resetImageWHToMap(exifMap,file,true);
            ExifInterface exif = new ExifInterface(file);
            Iterator<Map.Entry<String,String>> it = exifMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<String,String> entry = it.next();
                if(entry.getValue() != null){
                    try {
                        exif.setAttribute(entry.getKey(),entry.getValue());
                    }catch (Throwable throwable){
                        log(throwable);
                    }
                }
            }
            exif.saveAttributes();
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    public static void resetImageWHToMap(Map<String, String> exifMap, String file,boolean resetOritation) {
        if(exifMap.isEmpty()){
            log("exifMap.isEmpty");
            return;
        }
        File file1 = new File(file);
        if(!file1.exists()){
            log("file not exist:"+file);
            return;
        }
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(file, options); // 此时返回的bitmap为null
            exifMap.put("ImageLength",options.outHeight+"");
            exifMap.put("ImageWidth",options.outWidth+"");
            exifMap.put("PixelXDimension",options.outHeight+"");
            exifMap.put("PixelYDimension",options.outWidth+"");
            if(resetOritation){
                exifMap.put("Orientation",0+"");
            }

        }catch (Throwable throwable){
            log(throwable);
        }
    }

    private static void log(Object obj) {
        if(!LubanUtil.enableLog){
            return;
        }
        StringBuilder builder = new StringBuilder();
        if(obj instanceof Throwable){
            ((Throwable) obj).printStackTrace();
        }else if(obj instanceof Map){
            Iterator<Map.Entry> iterator = ((Map) obj).entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry entry = iterator.next();
                if(entry.getValue() != null){
                    builder.append(entry.getKey()+":"+entry.getValue()).append("\n");
                }
            }
        }else {
            builder.append(obj);
        }
        Log.d("exif",builder.toString());


    }
}
