package top.zibin.luban;

import java.io.File;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public interface ILubanConfig {

    void reportException(Throwable throwable);

    File getSaveDir();

    /**
     * 耗时和压缩百分比,上报到firebase的trace
     * @param timeCost
     * @param percent
     */
    void trace(long timeCost,int percent,long sizeAfterCompressInK);
}
