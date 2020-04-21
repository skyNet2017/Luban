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
}
