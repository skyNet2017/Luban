package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hss on 2018/12/14.
 */

public class DefaultBitmapToFile implements IBitmapToFile {

    static boolean isTrans(Bitmap bitmap,int x,int y){
        int pix = bitmap.getPixel(x,y);
        int a = ((pix >> 24) & 0xff) ;
        return a != 255;
    }
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
        //再判断一下4个角,看是否存在不透明度!=255的情况:
        if("image/png".equals(engine.originalMimeType)){
            engine.isPngWithTransAlpha =   isTrans(tagBitmap,0,0) ||
                    isTrans(tagBitmap,w,0)
                    || isTrans(tagBitmap,0,h)
                    || isTrans(tagBitmap,w,h);
        }


        if(luban.targetFormat.equals(Bitmap.CompressFormat.JPEG)){
            if(engine.isPngWithTransAlpha){
                //防止透明处变成黑色,而使用白色填充:
                Bitmap  bitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);

                long start = System.currentTimeMillis();
                out: for (int i = 0; i < w; i++) {
                    for (int j = 0; j < h; j++) {
                        // The argb {@link Color} at the specified coordinate
                        int pix = tagBitmap.getPixel(i,j);
                        int a = ((pix >> 24) & 0xff) ;/// 255.0f
                       // Log.d("luban","位置:"+i+"-"+j+", 不透明度:"+a);
                        //255就是没有透明度, = 0 就是完全透明. 值代表不透明度.值越大,越不透明
                        if(a != 255 ){
                            //半透明时,白色化,而不是简单粗暴地将不透明度设置为0:
                            /*float r = ((pix >> 16) & 0xff) / 255.0f;
                            float g = ((pix >>  8) & 0xff) / 255.0f;
                            float b = ((pix      ) & 0xff) / 255.0f;*/
                            //如果a=0,就是完全透明,那么将rgb设置为255.
                            //如果a>0 < 255,  就乘以(255-alpha)/255
                            if(a == 0){
                                pix = 0xffffffff;
                                //也可以改成外部传入背景色
                            }else {
                                //float  percent = (255-a)/255f;
                                pix = pix | 0xff000000;
                            }
                            // //那么看rgb是否为0,
                            // 如果不为0,就乘以(255-alpha)/255  //如果rgb都为0,黑色,无需改变
                            //pix = pix | 0xff000000;
                            bitmap.setPixel(i,j,pix);
                           // break out;
                        }else {
                            bitmap.setPixel(i,j,pix);
                        }
                        //FF: 255   00 : 0

                    }
                }
                Log.d("luban","透明通道白色化 cost(ms):"+(System.currentTimeMillis() - start));
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
