package com.hss01248.image.dataforphotoselet;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.blankj.utilcode.util.FileIOUtils;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.gzuliyujiang.filepicker.ExplorerConfig;
import com.github.gzuliyujiang.filepicker.FilePicker;
import com.github.gzuliyujiang.filepicker.annotation.ExplorerMode;
import com.github.gzuliyujiang.filepicker.contract.OnFilePickedListener;
import com.hss.utils.enhance.api.MyCommonCallback;
import com.hss01248.media.pick.CaptureImageUtil;
import com.hss01248.media.pick.MediaPickUtil;
import com.hss01248.media.uri.ContentUriUtil;

import org.devio.takephoto.wrap.TakeOnePhotoListener;
import org.devio.takephoto.wrap.TakePhotoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class ImgDataSeletor {


    public static void startPickOneWitchDialog(final FragmentActivity activity, final TakeOnePhotoListener listener) {
        try {
            final Dialog dialog = new Dialog(activity);
            dialog.getWindow().requestFeature(1);
            dialog.setContentView(R.layout.data_t_activity_select_pic);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = activity.getResources().getDisplayMetrics().widthPixels;
            dialog.getWindow().setAttributes(params);
            dialog.show();
            dialog.getWindow().findViewById(R.id.btn_take_photo).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    CaptureImageUtil.takePicture(false, new MyCommonCallback<String>() {
                        @Override
                        public void onSuccess(String s) {
                            listener.onSuccess(s);
                        }

                        @Override
                        public void onError(String msg) {
                            MyCommonCallback.super.onError(msg);
                            listener.onFail("",msg);
                        }
                    });
                }
            });
            dialog.getWindow().findViewById(R.id.btn_pick_photo2).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    MediaPickUtil.pickImage(new MyCommonCallback<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            //使用文件路径,低版本需要读的权限,高版本无法使用
                            //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                //ContentUriUtil.getRealPath(uri)
                            //}

                            //另一种方式: 拷贝到外部目录后,返回绝对路径. 如此可以使用bridge对接flutter,rn,无缝使用file path
                            //transUriToFilePathCallback(uri, listener);
                            listener.onSuccess(uri.toString());
                        }

                        @Override
                        public void onError(String msg) {
                            listener.onFail("",msg);
                        }
                    });
                }
            });
            dialog.getWindow().findViewById(R.id.btn_pick_photo_from_assets).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    selectAssertFile(activity, listener);
                }
            });
            dialog.getWindow().findViewById(R.id.btn_pick_file).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    selectFile(activity,true, listener);
                }
            });
            dialog.getWindow().findViewById(R.id.btn_pick_folder).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    selectFile(activity,false, listener);
                }
            });
            dialog.getWindow().findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            });
        } catch (Exception var4) {
            var4.printStackTrace();
        }
    }

    public static @Nullable File transUriToInnerFilePath(String pathOrUriString) {
        if(pathOrUriString.startsWith("content://")){
            Uri uri = Uri.parse(pathOrUriString);
            File dir  = Utils.getApp().getExternalFilesDir("pickCache");
            dir.mkdirs();
            Map<String, Object> infos = ContentUriUtil.getInfos(uri);
            String name = System.currentTimeMillis()+".jpg";
            if(infos.containsKey("_display_name")){
                name = infos.get("_display_name")+"";
            }
            File file = new File(dir,name);

            try {
                boolean success = FileIOUtils.writeFileFromIS(file, Utils.getApp().getContentResolver().openInputStream(uri));
                if(success && file.exists() && file.length() >0){
                    return file;
                }else {
                   return null;
                }
            } catch (Throwable e) {
                LogUtils.w(e);
                return null;
            }
        }else {
            File file = new File(pathOrUriString);
            if(file.exists() && file.length() >0){
                return file;
            }
            return null;
        }
    }

    public static @Nullable InputStream transUriToInputStream(String pathOrUriString) throws Exception{
        if(pathOrUriString.startsWith("content://")){
            Uri uri = Uri.parse(pathOrUriString);
           return Utils.getApp().getContentResolver().openInputStream(uri);
        }else {
            File file = new File(pathOrUriString);
            if(file.exists() && file.length() >0){
                return new FileInputStream(file);
            }
            return null;
        }
    }

    private static void selectFile(FragmentActivity activity,boolean isFile, TakeOnePhotoListener listener) {

        MediaPickUtil.pickMulti(new MyCommonCallback<List<Uri>>() {
            @Override
            public void onSuccess(List<Uri> uris) {
                //transUriToFilePathCallback(uris.get(0), listener);
                listener.onSuccess(uris.get(0).toString());
            }

            @Override
            public void onError(String code, String msg, @Nullable Throwable throwable) {
                MyCommonCallback.super.onError(code, msg, throwable);
                listener.onFail("",msg);
            }
        },false,"*/*");
    }


    volatile static List<File> files = new ArrayList<>();

    static void selectAssertFile(Context context, TakeOnePhotoListener listener) {
        if (files.isEmpty()) {
            fillFiles(context,listener);
        } else {
            showDialog(context, listener);
        }
    }

    private static void showDialog(Context context, TakeOnePhotoListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();

        RecyclerView recyclerView = new RecyclerView(context);
        dialog.setContentView(recyclerView);
        initRecycler(recyclerView, dialog, listener);
        dialog.show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            attributes.width = getAppScreenWidth(context);
            dialog.getWindow().setAttributes(attributes);
        }
    }

    public static int getAppScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return -1;
        Point point = new Point();
        wm.getDefaultDisplay().getSize(point);
        return point.x;
    }

    static AtomicInteger count = new AtomicInteger(0);
    private static void fillFiles(Context context, TakeOnePhotoListener listener) {
        try {
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(false);
            dialog.setTitle("下载图片中");
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            String host = "http://kodo.hss01248.tech/testimg/";
            List<String> paths = new ArrayList<>();
            paths.add("");
            paths.add("1600862676552-54801d6475b29239e55e90cf9666e388c3feef09464efcbea9a8593ff4f27321.gif");
            paths.add("0470f5d504444e4093c109939aa77b03_tplv-k3u1fbpfcp-zoom-1.gif");
            paths.add("2020-08-31-19-51-59-guihuada.gif");
            paths.add("20210331_105603_q_70.jpg");
            paths.add("342071-106.jpg");
            paths.add("QQ图片20180516180323.jpg");
            paths.add("Xnip2019-11-29_11-57-54.jpg");
            paths.add("b29b2e3e3a71835f241be55ba718b320.webp");
            paths.add("default_wallpaper.jpg");
            paths.add("documentimage_102722_30_03_2021_14_21_24.avif");
            paths.add("documentimage_104043_30_03_2021_11_55_00.avif");
            paths.add("f0a160c48cd0aad18f4610c9c799ac6.jpg");
            paths.add("fox.profile0.10bpc.yuv420.avif");
            paths.add("gif_雨.gif");
            paths.add("hato.profile0.10bpc.yuv420.avif");
            paths.add("jpg_10000x11852_一亿像素-麦田.jpg");
            paths.add("jpg_1231x800_思维导图.jpg");
            paths.add("jpg_2730x1536_鞋.jpg");
            paths.add("jpg_3600x1887_大图小字_地图.jpg");
            paths.add("jpg_5946x2258_喀纳斯广角.jpg");
            paths.add("jpg_720x236_广告条.jpg");
            paths.add("jpg_750x750_地图.jpg");
            paths.add("kimono.mirror-horizontal.avif");
            paths.add("png_5256x3324_logs-explorer-ui.png");
            paths.add("real_webp_fake_jpg.jpg");
            paths.add("splash_stars.jpeg");
            paths.add("test1.avif");
            paths.add("tmp-splash_stars.jpeg");
            paths.add("webp_724x408_鞋子.webp");
            paths.add("webp_大图小字-思维导图-增长黑客_2944x5434.webp");
            paths.add("webp_大图小字-思维导图2-领导力_2916x2233.webp");
            paths.add("婺源2.jpeg");
            paths.add("文件系统.jpg");


            paths.add("20210331_105603_q_70.jpg");
            paths.add("QQ图片20180516180323.jpg");
            paths.add("default_wallpaper.jpg");
            dialog.setMax(paths.size());
            count = new AtomicInteger(paths.size());
            File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "0lubanTestImgs");
            if(!dir.exists()){
                dir.mkdirs();
            }
            for (String path : paths) {
                ThreadUtils.executeByIo(new ThreadUtils.SimpleTask<File>() {
                    @Override
                    public File doInBackground() throws Throwable {
                        File target = new File(dir, path);
                        if(target.exists() && target.length()>0){
                            return target;
                        }
                        String url = host + path;

                            FutureTarget<File> submit = Glide.with(context)
                                    //.load(url)
                                    .downloadOnly()
                                    .load(Uri.parse(url))
                                    .submit();
                            File file = submit.get();
                            FileIOUtils.writeFileFromIS(target, new FileInputStream(file));
                            //refreshMediaCenter(context.getApplicationContext(), target.getAbsolutePath());
                        return target;
                    }

                    @Override
                    public void onSuccess(File result) {
                        LogUtils.d("文件下载并拷贝成功",host+path,result);
                        int i = count.decrementAndGet();
                        files.add(result);
                        dialog.setProgress(paths.size() -i);
                        if(i ==0){
                            dialog.dismiss();
                            showDialog(context, listener);
                        }
                    }

                    @Override
                    public void onFail(Throwable t) {
                        LogUtils.d("文件下载失败",host+path,t);
                        int i = count.decrementAndGet();
                        dialog.setProgress(paths.size() -i);
                        if(i ==0){
                            dialog.dismiss();
                            if(files.size() ==0){
                                ToastUtils.showLong("所有图片都下载失败了\n"+t.getCause().getMessage());
                                return;
                            }

                            showDialog(context, listener);
                        }
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initRecycler(RecyclerView recyclerView, final Dialog dialog, TakeOnePhotoListener listener) {
        BaseQuickAdapter adapter = new ImgAdapter2(R.layout.data_src_item_img);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);
        adapter.setNewData(files);
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                dialog.dismiss();
                listener.onSuccess(files.get(position).getAbsolutePath());
            }
        });


    }


    static void refreshMediaCenter(Context activity, String filePath) {
       /* File file  = new File(filePath);
        try {
            MediaStore.Images.Media.insertImage(activity.getContentResolver(),file.getAbsolutePath(), file.getName(), null);
        } catch (FileNotFoundException e) {
            ExceptionReporterHelper.reportException(e);
        }*/


        if (Build.VERSION.SDK_INT > 19) {
            String mineType = getMineType(filePath);

            saveImageSendScanner(activity, new MyMediaScannerConnectionClient(filePath, mineType));
        } else {

            saveImageSendBroadcast(activity, filePath);
        }
    }

    static String getMineType(String filePath) {

        String type = "text/plain";
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;


       /* MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String mime = "text/plain";
        if (filePath != null) {
            try {
                mmr.setDataSource(filePath);
                mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            } catch (IllegalStateException e) {
                return mime;
            } catch (IllegalArgumentException e) {
                return mime;
            } catch (RuntimeException e) {
                return mime;
            }
        }
        return mime;*/
    }

    /**
     * 保存后用广播扫描，Android4.4以下使用这个方法
     *
     * @author YOLANDA
     */
    private static void saveImageSendBroadcast(Context activity, String filePath) {
        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
    }

    /**
     * 保存后用MediaScanner扫描，通用的方法
     */
    private static void saveImageSendScanner(Context context, MyMediaScannerConnectionClient scannerClient) {

        final MediaScannerConnection scanner = new MediaScannerConnection(context, scannerClient);
        scannerClient.setScanner(scanner);
        scanner.connect();
    }


    private static class MyMediaScannerConnectionClient implements MediaScannerConnection.MediaScannerConnectionClient {

        private MediaScannerConnection mScanner;

        private String mScanPath;
        private String mimeType;

        public MyMediaScannerConnectionClient(String scanPath, String mimeType) {
            mScanPath = scanPath;
            this.mimeType = mimeType;
        }

        public void setScanner(MediaScannerConnection con) {
            mScanner = con;
        }

        @Override
        public void onMediaScannerConnected() {
            mScanner.scanFile(mScanPath, mimeType);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mScanner.disconnect();
        }
    }
}
