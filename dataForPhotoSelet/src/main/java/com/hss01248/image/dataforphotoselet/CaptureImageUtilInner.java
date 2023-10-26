package com.hss01248.image.dataforphotoselet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.Utils;

import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;
import com.hss01248.openuri2.OpenUri2;


import java.io.File;
import java.util.Arrays;
import java.util.List;


/**
 * @Despciption 一直以来，开发者只需要通过一定的条件去创建一个请求，就可以在相关的界面中选择系统推荐的第三方相机APP，供用户使用。
 * 但在 Android 11 中，有三项 Intent 功能不会再起作用，分别是视频拍摄（VIDEO_CAPTURE）、图像拍摄（IMAGE_CAPTURE）、
 * 密拍（IMAGE_CAPTURE_SECURE）。也就是说，如果用户升级了 Android 11 系统，那用户只能够使用自带的相机 APP 进行拍摄。
 * 谷歌对此解释，这样做「是为保护用户隐私和安全而作出的正确权衡」。
 * @Author hss
 * @Date 10/12/2021 14:14
 * @Version 1.0
 */
public class CaptureImageUtilInner {



    public static void takePicture(boolean useFrontCamera, MyCommonCallbackInner<String> callback){
        File externalFilesDir = Utils.getApp().getExternalFilesDir(Environment.DIRECTORY_DCIM);
        if(externalFilesDir == null){
            externalFilesDir = new File(Utils.getApp().getFilesDir(),Environment.DIRECTORY_DCIM);
        }
        externalFilesDir.mkdirs();
        File file = new File(externalFilesDir,System.currentTimeMillis()+".jpg");
        takePicture(useFrontCamera,file.getAbsolutePath(),callback);
    }

    public static void takePicture(boolean useFrontCamera,String path,  MyCommonCallbackInner<String> callback){

        PermissionUtils.permission(Manifest.permission.CAMERA)
                        .callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                videoCaptureIntent(useFrontCamera,path, callback);
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                callback.onError("no permission","need permissions:"
                        + Arrays.toString(denied.toArray()).replace("[","")
                        .replace("]","")
                        .replaceAll("android\\.permission\\.","")
                        .replace(",","")
                        .toLowerCase() ,null);
            }
        }).request();

    }

    private static void videoCaptureIntent(boolean useFrontCamera,String path,MyCommonCallbackInner<String> callback) {
        Intent intent=new Intent();
        // 指定开启系统相机的Action
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        if (useFrontCamera) {
            //intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        } else{
           // intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        // 根据文件地址创建文件
        File file=new File(path);
        // 把文件地址转换成Uri格式
        Uri uri= OpenUri2.fromFile(Utils.getApp(),file);
        OpenUri2.addPermissionRW(intent);
        // 设置系统相机拍摄照片完成后图片文件的存放地址
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

        //MediaStore.EXTRA_OUTPUT：设置媒体文件的保存路径。
        //MediaStore.EXTRA_VIDEO_QUALITY：设置视频录制的质量，0为低质量，1为高质量。
        //MediaStore.EXTRA_DURATION_LIMIT：设置视频最大允许录制的时长，单位为毫秒。
        //MediaStore.EXTRA_SIZE_LIMIT：指定视频最大允许的尺寸，单位为byte。

        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                LogUtils.d(resultCode,data);
                if(resultCode == Activity.RESULT_CANCELED){
                    callback.onError("cancel","you have canceled the recoding",null);
                    return;
                }
                if(resultCode != Activity.RESULT_OK){
                    LogUtils.w("result code is not RESULT_OK:"+resultCode);
                }
                if(file.exists() && file.length()> 0){
                    //MediaStoreRefresher.refreshMediaCenter(Utils.getApp(),path);
                    callback.onSuccess(path);
                }else {
                    callback.onError("file error","file saved error",null);
                }


            }

            @Override
            public void onActivityNotFound(Throwable e) {
                callback.onError("onActivityNotFound","no application to record video",e);
            }
        });
    }


}
