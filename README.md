# Luban



<div align="right">
<a href="Translation/README-EN.md">:book: English Documentation</a>
</div>

`Luban`（鲁班） —— `Android`图片压缩工具，仿微信朋友圈压缩策略。

`Luban-turbo` —— 鲁班项目的`turbo`版本，[查看`trubo`分支](https://github.com/Curzibn/Luban/tree/turbo)。



# 升级后:

## 使用

```
com.github.skyNet2017:Luban:1.2.5
```

Androidx版本:

```
com.github.skyNet2017:Luban:2.0.2
```

### LubanUtil

> 默认质量85. 不片面追求文件大小.
>
> 如果是聊天场景,可以自己设成65,最大可能节省大小.

![snapshot](snapshot.jpg)



### 有哪些优化



![优化](优化.jpg)

# 重复压缩问题

质量判断-仅适用于jpg文件,采用量化表计算整张图片像素. 



# exif处理

```
默认: boolean keepExif = true;
```

使用自己写的库ExifUtil 读写exif:

比it.sephiroth.android.exif:library:1.0.1通用性好. 底层基于androidx.exifinterface:exifinterface.

```
api 'com.github.hss01248:metadata:1.0.1'
```

>  压缩前读exif,压缩后写exif:

![image-20210205200135616](https://gitee.com/hss012489/picbed/raw/master/picgo/1612526495684-image-20210205200135616.jpg)



# 尺寸压缩时的损耗

> 尽量使用双线性插值代替默认的单线性插值

压缩插值算法效果对比见: https://cloud.tencent.com/developer/article/1006352

# OOM问题解决:

> 采用多次降级机制:

实现代码:

```java
//先使用双线性采样,oom了再使用单线性采样,还oom就强制压缩到720p
  private Bitmap compressBitmap() {
    //Luban.computeInSampleSize下限1080p
    int scale = Luban.computeInSampleSize(srcWidth,srcHeight);
    //获取原图的类型
    //String mimeType = options.outMimeType;
    //如果是png,看是否有透明的alpha通道,如果没有,给你压成jpg. 如果有,用白色填充.
    
    Bitmap tagBitmap2 = null;

    //计算个毛线,直接申请内存,oom了就降级:
    //压缩插值算法效果见: https://cloud.tencent.com/developer/article/1006352
    try {
      //使用双线性插值
      Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getPath());
      tagBitmap2 = Bitmap.createScaledBitmap(tagBitmap,srcWidth/scale,srcHeight/scale,true);
    }catch (OutOfMemoryError throwable){
      throwable.printStackTrace();
      try {
        //使用单线性插值
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = scale;
        //优先使用888. 因为RGB565在低版本手机上会变绿
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
      }catch (OutOfMemoryError throwable1){
        throwable1.printStackTrace();

        //用RGB_565, 如果原图是png,且有透明的alpha通道,那么会变黑. 如何处理?
        try {
          //使用RGB565将就一下:
          BitmapFactory.Options options = new BitmapFactory.Options();
          options.inSampleSize = scale;
          options.inPreferredConfig = Bitmap.Config.RGB_565;
          tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
          isPngWithTransAlpha = false;
        }catch (OutOfMemoryError error){
          error.printStackTrace();
          //try {
            //还TMD不行,只能压一把狠的:强制压缩到720p:
            int w = Math.min(srcHeight,srcWidth);
            scale =  w/720;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            tagBitmap2 = BitmapFactory.decodeFile(srcImg.getPath(), options);
            isPngWithTransAlpha = false;
          //}catch (OutOfMemoryError error2){
          //  error2.printStackTrace();
            //还TMD不行,老子不压了,返回原图: 在外面处理:
         // }
        }
      }
    }
    return tagBitmap2;
  }
```



# png转jpg变黑问题解决:

> alpha通道值代表不透明度,而非透明度.  完全透明就是0.

### png透明度判断:

![image-20210205171849446](https://gitee.com/hss012489/picbed/raw/master/picgo/1612516729504-image-20210205171849446.jpg)

### 判断算法:

算法性能:

* 限定长边100时,最大耗时400ms
* 限定50时,最大耗时82ms
* 最快: 四个角判断是否为0,   1ms即可.

![image-20210205191609366](https://gitee.com/hss012489/picbed/raw/master/picgo/1612523769414-image-20210205191609366.jpg)





### 转换算法:

![image-20210205191701186](https://gitee.com/hss012489/picbed/raw/master/picgo/1612523821231-image-20210205191701186.jpg)

### 压缩效果:

![image-20210205191329511](https://gitee.com/hss012489/picbed/raw/master/picgo/1612523609570-image-20210205191329511.jpg)

## 增加了一些trace,外部实现:

```
public void trace(long timeCost, int percent, long sizeAfterCompressInK, long width, long height) {
    LubanUtil.i("time cost(ms): "+timeCost+", filesize after compress:"+sizeAfterCompressInK +" , 压缩比:"+percent +"%,  wh:"+width+"-"+height);
}
```

















# 原库:

# 项目描述

目前做`App`开发总绕不开图片这个元素。但是随着手机拍照分辨率的提升，图片的压缩成为一个很重要的问题。单纯对图片进行裁切，压缩已经有很多文章介绍。但是裁切成多少，压缩成多少却很难控制好，裁切过头图片太小，质量压缩过头则显示效果太差。

于是自然想到`App`巨头“微信”会是怎么处理，`Luban`（鲁班）就是通过在微信朋友圈发送近100张不同分辨率图片，对比原图与微信压缩后的图片逆向推算出来的压缩算法。

因为有其他语言也想要实现`Luban`，所以描述了一遍[算法步骤](/DESCRIPTION.md)。

因为是逆向推算，效果还没法跟微信一模一样，但是已经很接近微信朋友圈压缩后的效果，具体看以下对比！

# 效果与对比

内容 | 原图 | `Luban` | `Wechat`
---- | ---- | ------ | ------
截屏 720P |720*1280,390k|720*1280,87k|720*1280,56k
截屏 1080P|1080*1920,2.21M|1080*1920,104k|1080*1920,112k
拍照 13M(4:3)|3096*4128,3.12M|1548*2064,141k|1548*2064,147k
拍照 9.6M(16:9)|4128*2322,4.64M|1032*581,97k|1032*581,74k
滚动截屏|1080*6433,1.56M|1080*6433,351k|1080*6433,482k

# 导入

```sh
implementation 'top.zibin:Luban:1.1.8'
```

# 使用

### 方法列表

方法 | 描述
---- | ----
load | 传入原图
filter | 设置开启压缩条件
ignoreBy | 不压缩的阈值，单位为K
setFocusAlpha | 设置是否保留透明通道 
setTargetDir | 缓存压缩图片路径
setCompressListener | 压缩回调接口
setRenameListener | 压缩前重命名接口

### 异步调用

`Luban`内部采用`IO`线程进行图片压缩，外部调用只需设置好结果监听即可：

```java
Luban.with(this)
        .load(photos)
        .ignoreBy(100)
        .setTargetDir(getPath())
        .filter(new CompressionPredicate() {
          @Override
          public boolean apply(String path) {
            return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
          }
        })
        .setCompressListener(new OnCompressListener() {
          @Override
          public void onStart() {
            // TODO 压缩开始前调用，可以在方法内启动 loading UI
          }

          @Override
          public void onSuccess(File file) {
            // TODO 压缩成功后调用，返回压缩后的图片文件
          }

          @Override
          public void onError(Throwable e) {
            // TODO 当压缩过程出现问题时调用
          }
        }).launch();
```

### 同步调用

同步方法请尽量避免在主线程调用以免阻塞主线程，下面以rxJava调用为例

```java
Flowable.just(photos)
    .observeOn(Schedulers.io())
    .map(new Function<List<String>, List<File>>() {
      @Override public List<File> apply(@NonNull List<String> list) throws Exception {
        // 同步方法直接返回压缩后的文件
        return Luban.with(MainActivity.this).load(list).get();
      }
    })
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe();
```





### RELEASE NOTE

[Here](https://github.com/Curzibn/Luban/releases)

# License

    Copyright 2016 Zheng Zibin
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
