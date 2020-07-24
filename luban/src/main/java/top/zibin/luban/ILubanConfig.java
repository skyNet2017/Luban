package top.zibin.luban;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public interface ILubanConfig {

   default void reportException(Throwable throwable){
       if(LubanUtil.enableLog){
           throwable.printStackTrace();
       }
   }

  default   File getSaveDir(){
       return LubanUtil.app.getExternalFilesDir(Environment.DIRECTORY_DCIM);
  }

    /**
     * 耗时和压缩百分比,上报到firebase的trace
     * @param timeCost
     * @param percent
     */
   default void trace(long timeCost,int percent,long sizeAfterCompressInK,long width,long height){
       Log.d("traceLubann","timeCost:"+timeCost+",compressed percent:"+percent+
               ",sizeAfterCompressInK:"+sizeAfterCompressInK+",w-h:"+width+"x"+height);
   }

    default  boolean useARGB888(){
        return false;
    }

    default  boolean keepExif(){
        return false;
    }
}
