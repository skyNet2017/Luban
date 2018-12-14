package com.hss01248.lubanturbo;

import top.zibin.luban.Luban;

/**
 * Created by hss on 2018/12/14.
 */

public class LubanTurbo extends Luban {
    protected LubanTurbo(Builder builder) {
        super(builder);
        this.bitmapToFile = TurboCompressor.getTurboCompressor();
    }
}
