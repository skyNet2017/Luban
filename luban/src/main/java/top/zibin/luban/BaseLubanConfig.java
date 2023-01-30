package top.zibin.luban;

import com.blankj.utilcode.util.LogUtils;


import java.io.File;

/**
 * by hss
 * data:2020-04-21
 * desc:
 */
public class BaseLubanConfig implements ILubanConfig {
    @Override
    public void reportException(Throwable throwable) {
        if (LubanUtil.enableLog) {
            LogUtils.w(throwable);
        }
    }


}
