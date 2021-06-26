package com.hss01248.luban.showresult;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.hss01248.media.metadata.ExifUtil;
import com.hss01248.media.metadata.quality.Magick;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.Map;

public class ShowResultUtil {
    public static boolean showCompressResult = false;


    public static void showResult(Activity activity, String inputPath, String outputPath, long timeCost, int percent, long sizeAfterCompressInK, long width, long height) {
        if (!showCompressResult) {
            return;
        }
        String desc = inputPath + "\ncompress to --> " + outputPath + "\ntime cost : " + timeCost + "ms, filesize after compress:"
                + sizeAfterCompressInK + "kB , 减少掉:" + percent + "%,  wh:" + width + "x" + height;
        ViewGroup root = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.luban_result_dialog,
                (ViewGroup) activity.getWindow().getDecorView(), false);
        TextView tvDesc = root.findViewById(R.id.tv_desc);
        TextView tvOriginal = root.findViewById(R.id.tv_original);
        TextView tvCompressed = root.findViewById(R.id.tv_compressed);
        SubsamplingScaleImageView ivOriginal = root.findViewById(R.id.iv_original);
        SubsamplingScaleImageView ivComressed = root.findViewById(R.id.iv_compressed);

        tvDesc.setText(desc);

        ivOriginal.setImage(ImageSource.uri(Uri.fromFile(new File(inputPath))));
        tvOriginal.setText("原图(点击显示exif:):" + getImgInfo(inputPath));
        tvOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExif(activity, inputPath);
            }
        });

        ivComressed.setImage(ImageSource.uri(Uri.fromFile(new File(outputPath))));
        tvCompressed.setText("压缩后(点击显示exif:):" + getImgInfo(outputPath));
        tvCompressed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExif(activity, outputPath);
            }
        });

        Dialog dialog = new AlertDialog.Builder(activity)
                .setTitle("压缩结果")
                .setView(root)
                .setPositiveButton("ok", null)
                .create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialog.show();


    }


    public static void showExif(Activity activity, String compress) {
        try {

            String str = ExifUtil.getExifStr(compress);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(compress)
                    .setMessage(str)
                    .setPositiveButton("ok", null)
                    .create();
            dialog.show();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    public static String getImgInfo(String path) {
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
    }

    public static int guessQuality(String path) {
        try {
            return new Magick().getJPEGImageQuality(new FileInputStream(path));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return 0;
        }
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
