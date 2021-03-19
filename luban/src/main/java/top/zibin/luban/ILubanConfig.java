package top.zibin.luban;

import java.io.File;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public interface ILubanConfig {

    void reportException(Throwable throwable);

    /**
     * 使用应用内部的路径,避免被app提示
     * @return
     */
    File getSaveDir();

    /**
     * 耗时和压缩百分比,上报到firebase的trace
     * @param timeCost
     * @param percent
     */
   default void trace(String inputPath,String outputPath,long timeCost,int percent,long sizeAfterCompressInK,long width,long height){
       LubanUtil.i(inputPath+"\ncompress to --> "+outputPath+"\ntime cost : "+timeCost+"ms, filesize after compress:"
               +sizeAfterCompressInK +"kB , 减少掉:"+percent +"%,  wh:"+width+"x"+height);
   }
}
