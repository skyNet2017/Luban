package top.zibin.luban.example;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.hss01248.media.metadata.ExifUtil;

import it.sephiroth.android.library.exif2.ExifInterface;
import org.devio.takephoto.wrap.TakeOnePhotoListener;
import org.devio.takephoto.wrap.TakePhotoUtil;



import top.zibin.luban.ILubanConfig;
import top.zibin.luban.Luban;
import top.zibin.luban.LubanUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LubanUtil.init(getApplication(), true, null);
        //stickMode();
        setContentView(R.layout.activity_comprare);
        initView();
        initEvent();
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
        TakePhotoUtil.startPickOneWitchDialog(this, new TakeOnePhotoListener() {
            @Override
            public void onSuccess(final String path) {
                ivOriginal.setImage(ImageSource.uri(Uri.fromFile(new File(path))));
                tvOriginal.setText("原图(点击显示exif:):"+getImgInfo(path));
                tvOriginal.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showExif(path);
                    }
                });
                compress(path);
            }

            @Override
            public void onFail(String path, String msg) {
                Log.d("d",msg);
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
        long  size = new File(path).length();
        String sizeStr = formatFileSize(size);

        int quality = guessQuality(path);

        return srcWidth + "x"+srcHeight+","+sizeStr+",Q:"+quality+"\npath: "+path;
    }

    private int guessQuality(String path) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif( path, ExifInterface.Options.OPTION_ALL );
            return   exif.getQualityGuess();

        } catch (Throwable e) {
            e.printStackTrace();
            return 0;

        }
    }

    private void compress(String path) {
        try {
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
            tvLuban.setText("compressForMaterialUpload(点击显示exif):\n"+getImgInfo(compress));
            tvLuban.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress);

                }
            });

            final String compress0 = LubanUtil.compressForMaterialUploadWebp(path).getAbsolutePath();
            ivwebp.setImage(ImageSource.uri(Uri.fromFile(new File(compress0))));
            tvwebp.setText("webp(点击显示exif):\n"+getImgInfo(compress0));
            tvwebp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress0);

                }
            });


            final String compress2 = LubanUtil.compressForNormalUsage(path).getAbsolutePath();
            ivLubanTurbo.setImage(ImageSource.uri(Uri.fromFile(new File(compress2))));
            tvLubanTurbo.setText("compressForNormalUsage(点击显示exif):\n"+getImgInfo(compress2));
            tvLubanTurbo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress2);

                }
            });

            final String compress3 = LubanUtil.compressWithNoResize(path).getAbsolutePath();
            ivNoResize.setImage(ImageSource.uri(Uri.fromFile(new File(compress3))));
            tvNoResize.setText("compressWithNoResize(点击显示exif):\n"+getImgInfo(compress3));
            tvNoResize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExif(compress3);

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
            /*List<File> files2 =  Luban.with(this)
                    .load(path)
                    .ignoreBy(5)
                    .targetQuality(65)
                    .targetFormat(Bitmap.CompressFormat.WEBP)
                    // .ignoreBy(40)
                    .get();
            String compress2 = files2.get(0).getAbsolutePath();
            ivLubanTurbo.setImage(ImageSource.uri(Uri.fromFile(new File(compress2))));
            tvLubanTurbo.setText("luban-webp-65:"+getImgInfo(compress2));

            Log.w("exif",ExifUtil.readExif(new FileInputStream(files2.get(0))).toString());*/
           // Log.w("meta",MetaDataUtil.getAllInfo(compress2).toString());

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private void showExif(String compress) {
        try {
            Map<String, String> map = ExifUtil.readExif(new FileInputStream(new File(compress)));
            String str = map.toString().replace(",","\n");
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(compress)
                    .setMessage(str)
                    .setPositiveButton("ok",null)
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
