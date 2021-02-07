package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hss on 2018/12/14.
 */

public class DefaultBitmapToFile implements IBitmapToFile {



    @Override
    public File compressToFile(Bitmap tagBitmap, File tagImg, boolean focusAlpha, int quality, Luban luban, Engine engine) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if(luban.targetFormat.equals(Bitmap.CompressFormat.WEBP)){
            //https://developer.android.com/reference/android/graphics/Bitmap.CompressFormat
            //Compress to the WEBP format. quality of 0 means compress for the smallest size.
            // 100 means compress for max visual quality. As of Build.VERSION_CODES.Q,
            // a value of 100 results in a file in the lossless WEBP format.
            // Otherwise the file will be in the lossy WEBP format.
            //100的会比99大很多,非常不划算.
            //webp可用于算法模型分析.因为jpg格式压缩会损失非常多的精度,极大影响图像相似度比对的结果
            //quality = 99;
        }
        int w = tagBitmap.getWidth();
        int h = tagBitmap.getHeight();
        //采样,看是否存在不透明度!=255的情况:
        if("image/png".equals(engine.originalMimeType) && !engine.isPngWithTransAlpha){
            long start2 = System.currentTimeMillis();
            engine.isPngWithTransAlpha =   LubanUtil.hasTransInAlpha(tagBitmap);
            LubanUtil.d("hastrans: cost(ms):"+(System.currentTimeMillis() - start2));
        }

        //engine.isPngWithTransAlpha = false;

        if(luban.targetFormat.equals(Bitmap.CompressFormat.JPEG)){
            if(engine.isPngWithTransAlpha){
                //原bitmap是imutable,不能直接更改像素点,要新建bitmap,像素编辑后设置
                Bitmap  bitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);

                long start = System.currentTimeMillis();
                out: for (int i = 0; i < w; i++) {
                    for (int j = 0; j < h; j++) {
                        // The argb {@link Color} at the specified coordinate
                        int pix = tagBitmap.getPixel(i,j);
                        long alpha = ((pix >> 24) & 0xff) ;/// 255.0f
                       // Log.d("luban","位置:"+i+"-"+j+", 不透明度:"+a);
                        //255就是没有透明度, = 0 就是完全透明. 值代表不透明度.值越大,越不透明
                        if(alpha != 255 ){
                            //半透明时,白色化,而不是像Android原生内部实现一样简单粗暴地将不透明度设置为0,一片黑色

                            //策略1: 不混合颜色,只区分0和255.只要有半透明,就使用前景色  性能还可以
                           /* if(alpha == 0){
                                luban.tintBgColorIfHasTransInAlpha = luban.tintBgColorIfHasTransInAlpha | 0xff000000;
                                pix = luban.tintBgColorIfHasTransInAlpha ;
                                //也可以改成外部传入背景色
                            }else {
                               pix = pix | 0xff000000;
                            }*/

                            //策略2: 颜色混合:  显示颜色= 前景色* alpha/255 + 背景色 * (255 - alpha)/255.
                            if(alpha == 0){
                                //将alpha改成255.完全不透明
                                pix = luban.tintBgColorIfHasTransInAlpha | 0xff000000;
                            }else {
                               /* 要使用rgb三个通道分别计算,而不能作为一个int值整体计算:
                               long pix2 = (long) (pix * alpha/255f +  luban.tintBgColorIfHasTransInAlpha * (255f-alpha) / 255f);
                                pix2 = pix2 | 0xff000000; */

                                int r = ((pix >> 16) & 0xff);
                                int g = ((pix >>  8) & 0xff);
                                int b = ((pix      ) & 0xff);

                                int br = ((luban.tintBgColorIfHasTransInAlpha >> 16) & 0xff);
                                int bg = ((luban.tintBgColorIfHasTransInAlpha >>  8) & 0xff);
                                int bb = ((luban.tintBgColorIfHasTransInAlpha      ) & 0xff);

                                int fr = Math.round((r * alpha +  br * (255-alpha)) / 255f);
                                int fg = Math.round((g * alpha +  bg * (255-alpha)) / 255f);
                                int fb = Math.round((b * alpha +  bb * (255-alpha)) / 255f);

                                // 注意是用或,不是用加: pix = 0xff << 24 + fr << 16 + fg << 8 + fb;
                                pix =  (0xff << 24) | (fr << 16) | (fg << 8) | fb;
                                //等效: Color.argb(0xff,fr,fg,fb);
                            }
                            bitmap.setPixel(i,j,pix);
                        }else {
                            bitmap.setPixel(i,j,pix);
                        }
                    }
                }
                LubanUtil.d("半透明通道颜色混合 cost(ms):"+(System.currentTimeMillis() - start));
                tagBitmap = bitmap;
            }

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
