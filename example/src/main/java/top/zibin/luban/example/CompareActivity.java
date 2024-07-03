package top.zibin.luban.example;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.hss01248.image.dataforphotoselet.ImgAdapter2;
import com.hss01248.image.dataforphotoselet.ImgDataSeletor;
import com.hss01248.luban.avif.AvifComressor;
import com.hss01248.media.metadata.ExifUtil;
import com.hss01248.media.metadata.FileTypeUtil;

import org.devio.takephoto.wrap.TakeOnePhotoListener;
import org.devio.takephoto.wrap.TakePhotoUtil;


import top.zibin.luban.DefaultRenameListener;
import top.zibin.luban.Luban;
import top.zibin.luban.LubanUtil;
import top.zibin.luban.example.quality.Magick;

import java.io.ByteArrayInputStream;
import java.io.File;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangshuisheng on 2019/1/23.
 */
public class CompareActivity extends AppCompatActivity {

    private Button btnChoose;

    private TextView tvOriginal;

    private SubsamplingScaleImageView ivOriginal;

    private TextView tvLuban;

    private SubsamplingScaleImageView ivLuban;

    private TextView tvLubanTurbo;

    private SubsamplingScaleImageView ivLubanTurbo;

    private TextView tvNoResize;

    private SubsamplingScaleImageView ivNoResize;

    private TextView tvwebp;

    private SubsamplingScaleImageView ivwebp;

    private TextView tvwebpNoResize;

    private SubsamplingScaleImageView ivwebpNoResize;
    private TextView tvturbo;
    private SubsamplingScaleImageView ivTurbo;
    List<File> files = new ArrayList<>();
    private Button btnSelf;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //stickMode();
        setContentView(R.layout.activity_comprare);
        initView();
        btnSelf = findViewById(R.id.btn_chooseself);
        btnSelf.setVisibility(View.GONE);
        initEvent();


    }


    public void api(View view) {
        RelativeLayout linearLayout = new RelativeLayout(this);
        ImageView imageView = new ImageView(this);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(params);
        linearLayout.addView(imageView);
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("apis")
                .setView(linearLayout)
                .setPositiveButton("ok", null).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        String path = "file:///android_asset/luban.jpg";
        //Uri imageUri = Uri.fromFile(new File("//android_asset/luban.jpg"));
        dialog.show();
        Glide.with(this)
                .load(path)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        e.printStackTrace();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.i("ss", "onResourceReady:" + model);
                        return false;
                    }
                })
                .into(imageView);

    }


    private void stickMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()//开启所有的detectXX系列方法
                //.penaltyDialog()//弹出违规提示框
                .penaltyLog()//在Logcat中打印违规日志
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
    }

    private void initEvent() {
        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseAndShow();
            }
        });
    }

    private void chooseAndShow() {

        // ImgDataSeletor.startPickOneWitchDialog(thism);


        ImgDataSeletor.startPickOneWitchDialog(this, new TakeOnePhotoListener() {
            @Override
            public void onSuccess(final String path) {
                //LubanUtil.compressOriginal(path, 88);

                ExifInterface exifInterface = null;
                try {
                    exifInterface = new ExifInterface(path);
                    byte[] thumbnailBytes = exifInterface.getThumbnailBytes();
                    if(thumbnailBytes != null){
                        String type = "uknonwn";
                        try {
                            type = FileTypeUtil.getType(new ByteArrayInputStream(thumbnailBytes));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        LogUtils.w("thumbnailBytes:"+thumbnailBytes.length+",type: "+type);
                    }else{
                        LogUtils.w("no thumbnail:");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


                compress(path);
            }

            @Override
            public void onFail(String path, String msg) {
                Log.d("d", msg);
                ToastUtils.showLong(msg);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    private String getImgInfo(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;

        BitmapFactory.decodeFile(path, options);
        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;
        long size = new File(path).length();
        String sizeStr = formatFileSize(size);

        int quality = guessQuality(path);

        return srcWidth + "x" + srcHeight + "," + sizeStr + ",Q:" + quality + "\npath: " + path;
        //return ExifUtil.getExifStr(path);
    }

    private int guessQuality(String path) {
        return LubanUtil.getJpegQuality(path);
    }

    private void compress(final String path) {
        try {

            ivOriginal.setImage(ImageSource.uri(Uri.fromFile(new File(path))));
            tvOriginal.setText("原图(点击显示exif:):" + getImgInfo(path));
            tvOriginal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(path);
                }
            });
            //ExifUtil.readExif(new FileInputStream(path));
         /* List<File> files =  Luban.with(this)
                    .load(path)
                  .targetQuality(85)
                  .ignoreBy(1)
                  .tintBgColorIfHasTransInAlpha(0xff00ff)
                  //.keepExif(true)
                  .targetFormat(Bitmap.CompressFormat.JPEG)
                 // .ignoreBy(40)
                    .get();
          String compress = files.get(0).getAbsolutePath();*/
            final String compress = LubanUtil.compressForMaterialUpload(path).getAbsolutePath();



            ivLuban.setImage(ImageSource.uri(Uri.fromFile(new File(compress))));
            tvLuban.setText("compressForMaterialUpload(点击显示exif):\n" + getImgInfo(compress));
            tvLuban.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress);

                }
            });

            final String compress0 = LubanUtil.compressForMaterialUploadWebp(path).getAbsolutePath();

            //todo webp被ExifInterface操作后,图片被部分损坏,无法显示在glide中,无法被chrome正确显示,无法被Image.io读取
            ExifInterface exifInterface = new ExifInterface(compress0);
            exifInterface.setAttribute(ExifInterface.TAG_SOFTWARE,"dddd");
            exifInterface.setAttribute(ExifInterface.TAG_MAKE,"77777");
            byte[] thumbnailBytes = exifInterface.getThumbnailBytes();
            if(thumbnailBytes != null){
                LogUtils.w("thumbnailBytes:"+thumbnailBytes.length);
            }

            try {
                exifInterface.saveAttributes();
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }

            ivwebp.setImage(ImageSource.uri(Uri.fromFile(new File(compress0))));
            tvwebp.setText("webp(点击显示exif):\n" + getImgInfo(compress0));
            tvwebp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress0);

                }
            });


            final String compress3 = Luban.with(getApplicationContext())
                    //.ignoreBy(MIN_IMAGE_COMPRESS_SIZE)
                    .targetQuality(75)
                    .keepExif(true)
                    .targetFormat(Bitmap.CompressFormat.WEBP)
                    .noResize(true)
                    .get(path).getAbsolutePath();


            ivwebpNoResize.setImage(ImageSource.uri(Uri.fromFile(new File(compress3))));
            tvwebpNoResize.setText("webp(点击显示exif):\n" + getImgInfo(compress3));
            tvwebpNoResize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress3);

                }
            });


            final String compress2 = LubanUtil.compressForNormalUsage(path).getAbsolutePath();
            ivLubanTurbo.setImage(ImageSource.uri(Uri.fromFile(new File(compress2))));
            tvLubanTurbo.setText("compressForNormalUsage(75-点击显示exif):\n" + getImgInfo(compress2));
            tvLubanTurbo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress2);

                }
            });

            final String compress4 = Luban.with(getApplicationContext())
                    .targetQuality(75)
                    .keepExif(true)
                    .noResize(true)
                   // .setTargetDir(config.getSaveDir().getAbsolutePath())
                    .get(path).getAbsolutePath();
            ivNoResize.setImage(ImageSource.uri(Uri.fromFile(new File(compress4))));
            tvNoResize.setText("compressWithNoResize(点击显示exif):\n" + getImgInfo(compress4));
            tvNoResize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress4);

                }
            });


            //ExifUtil.readExif(new FileInputStream(files.get(0)));
            //Log.w("meta",MetaDataUtil.getAllInfo(compress).toString());

            //{ImageWidth=1560, ImageLength=2080, Orientation=0, LightSource=0}

            /*List<File> files2 =  Luban.with(this)
                    .load(path)
                   // .ignoreBy(40)
                    //.saver(TurboCompressor.getTurboCompressor())//rgb565会导致crash
                    .get();*/
            ThreadUtils.executeByCpu(new ThreadUtils.SimpleTask<File>() {
                @Override
                public File doInBackground() throws Throwable {
                    File files66 = Luban.with(CompareActivity.this)
                            .ignoreBy(50)
                            .toAvif()
                            .setCompressor(new AvifComressor())
                            .targetQuality(80)
                            .maxShortDimension(1080)
                            //.noResize(true)
                            .get(path);
                    return files66;
                }

                @Override
                public void onSuccess(File result) {
                    Glide.with(CompareActivity.this)
                            .asBitmap()
                            .load(result)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    ivTurbo.setImage(ImageSource.bitmap(resource));
                                    tvturbo.setText("Avif (点击显示exif):\n" + getImgInfo(result.getAbsolutePath()));
                                    tvturbo.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            showExif(result.getAbsolutePath());

                                        }
                                    });
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            });
                }
            });


           // ivTurbo.setImage(ImageSource.uri(Uri.fromFile(new File(compress66))));


        } catch (Throwable e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //compressDifferentQualitys(path);
            }
        }).start();


    }

    private void compressDifferentQualitys(String path) {
        for (int i = 0; i <= 100; i += 5) {
            int finalI = i;
            File in = Luban.with(this)
                    .ignoreBy(10)
                    .targetQuality(i)
                    .setRenameListener(new DefaultRenameListener() {
                        @Override
                        public String rename(String filePath) {
                            try {
                                return filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))
                                        + "_q_" + finalI + filePath.substring(filePath.lastIndexOf("."));
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                                return new File(filePath).getName();
                            }
                        }
                    })
                    .maxShortDimension(1080)
                    .get(path);
            //  ExifInterface exif = new ExifInterface();
            int quality1 = 0;
            try {
                // exif.readExif(in.getAbsolutePath(), ExifInterface.Options.OPTION_ALL);
                //  quality1 = exif.getQualityGuess();
                int quality2 = new Magick().getJPEGImageQuality(in);
                Log.w("quality", in.getName() + " compress q:" + i + ", real q1:" + quality1 + ", real q2:" + quality2);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void showExif(String compress) {
        try {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(compress)
                    .setMessage(ExifUtil.getExifStr(compress))
                    .setPositiveButton("ok", null)
                    .create();
            dialog.show();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        btnChoose = (Button) findViewById(R.id.btn_choose);
        tvOriginal = (TextView) findViewById(R.id.tv_original);
        ivOriginal = (SubsamplingScaleImageView) findViewById(R.id.iv_original);
        tvLuban = (TextView) findViewById(R.id.tv_luban);
        ivLuban = (SubsamplingScaleImageView) findViewById(R.id.iv_luban);
        tvLubanTurbo = (TextView) findViewById(R.id.tv_luban_turbo);
        ivLubanTurbo = (SubsamplingScaleImageView) findViewById(R.id.iv_luban_turbo);
        tvNoResize = (TextView) findViewById(R.id.tv_noresize);
        ivNoResize = (SubsamplingScaleImageView) findViewById(R.id.iv_noresize);
        tvwebp = (TextView) findViewById(R.id.tv_webp);
        ivwebp = (SubsamplingScaleImageView) findViewById(R.id.iv_webp);
        tvturbo = findViewById(R.id.tv_jpgturbo);
        ivTurbo = findViewById(R.id.iv_jpgturbo);

        tvwebpNoResize = (TextView) findViewById(R.id.tv_webp_no_resize);
        ivwebpNoResize = (SubsamplingScaleImageView) findViewById(R.id.iv_webp_noresize);
    }

    public static String formatFileSize(long size) {
        try {
            DecimalFormat dff = new DecimalFormat(".00");
            if (size >= 1024 * 1024) {
                double doubleValue = ((double) size) / (1024 * 1024);
                String value = dff.format(doubleValue);
                return value + "MB";
            } else if (size > 1024) {
                double doubleValue = ((double) size) / 1024;
                String value = dff.format(doubleValue);
                return value + "KB";
            } else {
                return size + "B";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(size);
    }


}
