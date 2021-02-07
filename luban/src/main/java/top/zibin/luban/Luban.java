package top.zibin.luban;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unused")
public class Luban implements Handler.Callback {
  private static final String TAG = "Luban";
  private static final String DEFAULT_DISK_CACHE_DIR = "luban_disk_cache";

  private static final int MSG_COMPRESS_SUCCESS = 0;
  private static final int MSG_COMPRESS_START = 1;
  private static final int MSG_COMPRESS_ERROR = 2;
  public static final int TARGET_QUALITY = 65;

   String mTargetDir;
   boolean focusAlpha;
  boolean keepExif;
   int mLeastCompressSize;
  int tintBgColorIfHasTransInAlpha;
   OnRenameListener mRenameListener;
   OnCompressListener mCompressListener;
   CompressionPredicate mCompressionPredicate;
   List<InputStreamProvider> mStreamProviders;
   IBitmapToFile bitmapToFile;

   Bitmap.CompressFormat targetFormat;
   int maxShortDimension;
  /**
   * 压缩的目标质量,65
   */
  private int quality = TARGET_QUALITY;

  private Handler mHandler;

  protected Luban(Builder builder) {
    this.mTargetDir = builder.mTargetDir;
    this.mRenameListener = builder.mRenameListener;
    this.mStreamProviders = builder.mStreamProviders;
    this.mCompressListener = builder.mCompressListener;
    this.mLeastCompressSize = builder.mLeastCompressSize;
    this.mCompressionPredicate = builder.mCompressionPredicate;
    this.bitmapToFile = builder.bitmapToFile;
    this.quality = builder.quality;
    if(bitmapToFile == null){
      bitmapToFile = engine;
    }
    if(bitmapToFile == null){
      bitmapToFile = new DefaultBitmapToFile();
    }
    mHandler = new Handler(Looper.getMainLooper(), this);
    this.targetFormat = builder.targetFormat;
    this.keepExif = builder.keepExif;
    this.tintBgColorIfHasTransInAlpha = builder.tintBgColorIfHasTransInAlpha;
    this.maxShortDimension = builder.maxShortDimension;
  }

  private static IBitmapToFile engine;

  public static void init(IBitmapToFile engine){
    Luban.engine = engine;
  }

  /**
   * luban算法核心,计算一个合适的采样率  下限为1080p(长边1208)
   * @param srcWidth
   * @param srcHeight
   * @return
   */
  public static int computeInSampleSize(int srcWidth,int srcHeight) {
    srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
    srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

    int longSide = Math.max(srcWidth, srcHeight);
    int shortSide = Math.min(srcWidth, srcHeight);

    float scale = ((float) shortSide / longSide);
    if (scale <= 1 && scale > 0.5625) {
      if (longSide < 1664) {
        return 1;
      } else if (longSide < 4990) {
        return 2;
      } else if (longSide > 4990 && longSide < 10240) {
        return 4;
      } else {
        return longSide / 1280 == 0 ? 1 : longSide / 1280;
      }
    } else if (scale <= 0.5625 && scale > 0.5) {
      return longSide / 1280 == 0 ? 1 : longSide / 1280;
    } else {
      return (int) Math.ceil(longSide / (1280.0 / scale));
    }
  }

  public static Builder with(Context context) {
    return new Builder(context);
  }

  /**
   * Returns a file with a cache image name in the private cache directory.
   *
   * @param context A context.
   */
  private File getImageCacheFile(Context context, String suffix) {
    if (TextUtils.isEmpty(mTargetDir)) {
      mTargetDir = getImageCacheDir(context).getAbsolutePath();
    }
    if(targetFormat == Bitmap.CompressFormat.JPEG){
      suffix = ".jpg";
    }else  if(targetFormat == Bitmap.CompressFormat.WEBP){
      suffix = ".webp";
    }else if(targetFormat == Bitmap.CompressFormat.PNG){
      suffix = ".png";
    }

    String cacheBuilder = mTargetDir + "/" +
        System.currentTimeMillis() +
        (int) (Math.random() * 1000) +
        (TextUtils.isEmpty(suffix) ? ".jpg" : suffix);

    return new File(cacheBuilder);
  }

  private File getImageCustomFile(Context context, String filename) {
    if (TextUtils.isEmpty(mTargetDir)) {
      mTargetDir = getImageCacheDir(context).getAbsolutePath();
    }
    if(targetFormat == Bitmap.CompressFormat.JPEG){
      filename = filename.substring(0,filename.lastIndexOf("."))+ ".jpg";
    }else  if(targetFormat == Bitmap.CompressFormat.WEBP){
      filename = filename.substring(0,filename.lastIndexOf("."))+ ".webp";
    }else if(targetFormat == Bitmap.CompressFormat.PNG){
      filename = filename.substring(0,filename.lastIndexOf("."))+ ".png";
    }
    String cacheBuilder = mTargetDir + "/" + filename;

    return new File(cacheBuilder);
  }


  /**
   * Returns a directory with a default name in the private cache directory of the application to
   * use to store retrieved audio.
   *
   * @param context A context.
   * @see #getImageCacheDir(Context, String)
   */
  private File getImageCacheDir(Context context) {
    return getImageCacheDir(context, DEFAULT_DISK_CACHE_DIR);
  }

  /**
   * Returns a directory with the given name in the private cache directory of the application to
   * use to store retrieved media and thumbnails.
   *
   * @param context   A context.
   * @param cacheName The name of the subdirectory in which to store the cache.
   * @see #getImageCacheDir(Context)
   */
  private static File getImageCacheDir(Context context, String cacheName) {
    File cacheDir = context.getExternalCacheDir();
    if (cacheDir != null) {
      File result = new File(cacheDir, cacheName);
      if (!result.mkdirs() && (!result.exists() || !result.isDirectory())) {
        // File wasn't able to create a directory, or the result exists but not a directory
        return null;
      }
      return result;
    }
    if (Log.isLoggable(TAG, Log.ERROR)) {
      Log.e(TAG, "default disk cache dir is null");
    }
    return null;
  }

  /**
   * start asynchronous compress thread
   */
  private void launch(final Context context) {
    if (mStreamProviders == null || mStreamProviders.size() == 0 && mCompressListener != null) {
      mCompressListener.onError(new NullPointerException("image file cannot be null"));
    }

    Iterator<InputStreamProvider> iterator = mStreamProviders.iterator();

    while (iterator.hasNext()) {
      final InputStreamProvider path = iterator.next();

      AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
        @Override
        public void run() {
          try {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_START));

            File result = compress(context, path);

            mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, result));
          } catch (IOException e) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_ERROR, e));
          }
        }
      });

      iterator.remove();
    }
  }

  /**
   * start compress and return the file
   */
  private File get(InputStreamProvider input, Context context) throws IOException {
    //return new Engine(input, getImageCacheFile(context, Checker.SINGLE.extSuffix(input)), focusAlpha,bitmapToFile).compress();
    return compress(context,input);
  }

  private List<File> get(Context context) throws IOException {
    List<File> results = new ArrayList<>();
    Iterator<InputStreamProvider> iterator = mStreamProviders.iterator();

    while (iterator.hasNext()) {
      results.add(compress(context, iterator.next()));
      iterator.remove();
    }

    return results;
  }

  private File compress(Context context, InputStreamProvider path) throws IOException {
    File result;

    File outFile = getImageCustomFile(context, new File(path.getPath()).getName());
    //getImageCacheFile(context, Checker.SINGLE.extSuffix(path));
    if (mRenameListener != null) {
      String filename = mRenameListener.rename(path.getPath());
      outFile = getImageCustomFile(context, filename);
    }

    //加上日志:
    LubanUtil.logFile("compressByLuban begin",path.getPath());
    long start = System.currentTimeMillis();

    if (mCompressionPredicate != null) {
      if (mCompressionPredicate.apply(path.getPath())
          && Checker.SINGLE.needCompress(mLeastCompressSize,quality, path.getPath())) {
        result = new Engine(path, outFile, focusAlpha,bitmapToFile,quality,this).compress();
      } else {
        result = new File(path.getPath());
      }
    } else {
      result = Checker.SINGLE.needCompress(mLeastCompressSize,quality, path.getPath()) ?
          new Engine(path, outFile, focusAlpha,bitmapToFile,quality,this).compress() :
          new File(path.getPath());
    }

    //加上日志:

    long duration = System.currentTimeMillis() - start;
    LubanUtil.logFile("compressByLuban end",result.getAbsolutePath());
    LubanUtil.i("compressByLuban cost " + duration + " ms");

    int percent = 0;
    File file0 = new File(path.getPath());
    if(file0.exists() && result.exists() && file0.length() > 0){
      percent = (int) ((file0.length() - result.length()) * 100 / file0.length());
    }
    int[] wh = LubanUtil.getImageWidthHeight(result.getAbsolutePath());
    LubanUtil.config.trace(duration,percent,result.length()/1024,wh[0],wh[1]);

    return result;
  }

  @Override
  public boolean handleMessage(Message msg) {
    if (mCompressListener == null) return false;

    switch (msg.what) {
      case MSG_COMPRESS_START:
        mCompressListener.onStart();
        break;
      case MSG_COMPRESS_SUCCESS:
        mCompressListener.onSuccess((File) msg.obj);
        break;
      case MSG_COMPRESS_ERROR:
        mCompressListener.onError((Throwable) msg.obj);
        break;
    }
    return false;
  }

  public static class Builder {
    private Context context;
    private String mTargetDir;
    private boolean focusAlpha;
    private int mLeastCompressSize = 100;
    private OnRenameListener mRenameListener;
    private OnCompressListener mCompressListener;
    private CompressionPredicate mCompressionPredicate;
    private List<InputStreamProvider> mStreamProviders;
    private IBitmapToFile bitmapToFile;
    private int quality  = TARGET_QUALITY;
    private Bitmap.CompressFormat targetFormat = Bitmap.CompressFormat.JPEG;
    boolean keepExif = true;
    int tintBgColorIfHasTransInAlpha = 0x00ffffff;
    private int maxShortDimension;
    Builder(Context context) {
      this.context = context;
      this.mStreamProviders = new ArrayList<>();
    }

    private Luban build() {
      return new Luban(this);
    }

    public Builder load(InputStreamProvider inputStreamProvider) {
      mStreamProviders.add(inputStreamProvider);
      return this;
    }

    public Builder saver(IBitmapToFile bitmapToFile) {
      this.bitmapToFile = bitmapToFile;
      return this;
    }

    /**
     * 最短边的边长上限
     * 比如可以限制上线为720或者1080
     * @param maxShortDimension
     * @return
     */
    public Builder maxShortDimension(int maxShortDimension) {
      this.maxShortDimension = maxShortDimension;
      return this;
    }

    /**
     * 当为png,有半透明像素时,转为jpg时使用什么颜色来计算最终颜色
     * 格式: 0xff00ff. 不要传带透明通道的值
     * @param tintBgColorIfHasTransInAlpha
     * @return
     */
    public Builder tintBgColorIfHasTransInAlpha(int tintBgColorIfHasTransInAlpha) {
      this.tintBgColorIfHasTransInAlpha = tintBgColorIfHasTransInAlpha   ;
      return this;
    }
    public Builder targetQuality(int quality) {
      this.quality = quality;
      return this;
    }
    public Builder targetFormat(Bitmap.CompressFormat targetFormat) {
      this.targetFormat = targetFormat;
      return this;
    }
    public Builder keepExif(boolean keepExif) {
      this.keepExif = keepExif;
      return this;
    }

    public Builder load(final File file) {
      mStreamProviders.add(new InputStreamProvider() {
        @Override
        public InputStream open() throws IOException {
          return new FileInputStream(file);
        }

        @Override
        public String getPath() {
          return file.getAbsolutePath();
        }
      });
      return this;
    }

    public Builder load(final String string) {
      mStreamProviders.add(new InputStreamProvider() {
        @Override
        public InputStream open() throws IOException {
          return new FileInputStream(string);
        }

        @Override
        public String getPath() {
          return string;
        }
      });
      return this;
    }

    public <T> Builder load(List<T> list) {
      for (T src : list) {
        if (src instanceof String) {
          load((String) src);
        } else if (src instanceof File) {
          load((File) src);
        } else if (src instanceof Uri) {
          load((Uri) src);
        } else {
          throw new IllegalArgumentException("Incoming data type exception, it must be String, File, Uri or Bitmap");
        }
      }
      return this;
    }

    public Builder load(final Uri uri) {
      mStreamProviders.add(new InputStreamProvider() {
        @Override
        public InputStream open() throws IOException {
          return context.getContentResolver().openInputStream(uri);
        }

        @Override
        public String getPath() {
          return uri.getPath();
        }
      });
      return this;
    }

    public Builder putGear(int gear) {
      return this;
    }

    public Builder setRenameListener(OnRenameListener listener) {
      this.mRenameListener = listener;
      return this;
    }

    public Builder setCompressListener(OnCompressListener listener) {
      this.mCompressListener = listener;
      return this;
    }

    public Builder setTargetDir(String targetDir) {
      this.mTargetDir = targetDir;
      return this;
    }

    /**
     * Do I need to keep the image's alpha channel
     *
     * @param focusAlpha <p> true - to keep alpha channel, the compress speed will be slow. </p>
     *                   <p> false - don't keep alpha channel, it might have a black background.</p>
     */
    public Builder setFocusAlpha(boolean focusAlpha) {
      this.focusAlpha = focusAlpha;
      return this;
    }

    /**
     * do not compress when the origin image file size less than one value
     *
     * @param size the value of file size, unit KB, default 100K
     */
    public Builder ignoreBy(int size) {
      this.mLeastCompressSize = size;
      return this;
    }

    /**
     * do compress image when return value was true, otherwise, do not compress the image file
     *
     * @param compressionPredicate A predicate callback that returns true or false for the given input path should be compressed.
     */
    public Builder filter(CompressionPredicate compressionPredicate) {
      this.mCompressionPredicate = compressionPredicate;
      return this;
    }


    /**
     * begin compress image with asynchronous
     */
    public void launch() {
      build().launch(context);
    }

    public File get(final String path) throws IOException {
      return build().get(new InputStreamProvider() {
        @Override
        public InputStream open() throws IOException {
          return new FileInputStream(path);
        }

        @Override
        public String getPath() {
          return path;
        }
      }, context);
    }

    /**
     * begin compress image with synchronize
     *
     * @return the thumb image file list
     */
    public List<File> get() throws IOException {
      return build().get(context);
    }
  }
}