package top.zibin.luban;

public interface CompressType {

    /**
     * 给opencv,深度学习算法用的.
     * 可指定宽度上限. 比如720p, 1080p. 此分辨率对于算法已经足够大.
     * 尽量使用webp,且质量99
     * 如果一定要用jpg,质量设置为85.
     */
    int TYPE_FOR_ALG  = 1;

    /**
     * 用于人与人直接的交流.比如聊天图片,电商评价图片
     * 使用原生鲁班压缩倍数-近似微信. 也可以限定宽度上限,最大程度减小图片大小. 比如可统一限定到1080p
     * 质量使用65
     */
    int TYPE_FOR_CONMUNICATE  = 2;
}
