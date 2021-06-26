package com.hss01248.image.dataforphotoselet;

import android.app.Dialog;
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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.blankj.utilcode.util.FileIOUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;

import org.devio.takephoto.wrap.TakeOnePhotoListener;
import org.devio.takephoto.wrap.TakePhotoUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                    TakePhotoUtil.startPickOne(activity, true, listener);
                }
            });
            dialog.getWindow().findViewById(R.id.btn_pick_photo).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    TakePhotoUtil.startPickOne(activity, false, listener);
                }
            });
            dialog.getWindow().findViewById(R.id.btn_pick_photo_from_assets).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    selectAssertFile(activity,listener);
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








   volatile static List<File> files = new ArrayList<>();
     static void selectAssertFile(Context context,TakeOnePhotoListener listener){
        if(files.isEmpty()){
            Toast.makeText(context,"wait until the files copyed to sd card",Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fillFiles(context);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showDialog(context,listener);
                        }
                    },1000);
                }
            }).start();
        }else {
            showDialog(context,listener);
        }



    }

    private static void showDialog(Context context, TakeOnePhotoListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();

        RecyclerView recyclerView = new RecyclerView(context);
        dialog.setContentView(recyclerView);
        initRecycler(recyclerView,dialog,listener);
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

    private static void fillFiles(Context context) {
        try {
            String[] imgs = context.getAssets().list("imgsluban");
            File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"0lubanTestImgs");
            dir.mkdirs();
            Log.d("imgsdata", Arrays.toString(imgs));
            if(imgs==null || imgs.length ==0){
                Toast.makeText(context,"imgs is null in assets/imgsluban",Toast.LENGTH_LONG).show();
                return;
            }
            for (String img : imgs) {
                File target = new File(dir,img);
                files.add(target);
                FileIOUtils.writeFileFromIS(target,context.getAssets().open("imgsluban/"+img));
                refreshMediaCenter(context.getApplicationContext(),target.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initRecycler(RecyclerView recyclerView, final Dialog dialog, TakeOnePhotoListener listener) {
        BaseQuickAdapter adapter = new ImgAdapter2(R.layout.data_src_item_img);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));
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









     static  void refreshMediaCenter(Context activity, String filePath){
       /* File file  = new File(filePath);
        try {
            MediaStore.Images.Media.insertImage(activity.getContentResolver(),file.getAbsolutePath(), file.getName(), null);
        } catch (FileNotFoundException e) {
            ExceptionReporterHelper.reportException(e);
        }*/


        if (Build.VERSION.SDK_INT>19){
            String mineType =getMineType(filePath);

            saveImageSendScanner(activity,new MyMediaScannerConnectionClient(filePath,mineType));
        }else {

            saveImageSendBroadcast(activity,filePath);
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
     * @author YOLANDA
     */
    private static void saveImageSendBroadcast(Context activity, String filePath){
        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
    }

    /**
     * 保存后用MediaScanner扫描，通用的方法
     *
     */
    private static void saveImageSendScanner (Context context, MyMediaScannerConnectionClient scannerClient) {

        final MediaScannerConnection scanner = new MediaScannerConnection(context, scannerClient);
        scannerClient.setScanner(scanner);
        scanner.connect();
    }



    private   static class MyMediaScannerConnectionClient implements MediaScannerConnection.MediaScannerConnectionClient {

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
