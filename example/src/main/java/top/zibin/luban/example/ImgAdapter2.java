package top.zibin.luban.example;

import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.io.File;

public class ImgAdapter2 extends BaseQuickAdapter<File, BaseViewHolder> {
    public ImgAdapter2(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, File item) {
        Log.w("path",item.getAbsolutePath());
        Glide.with(helper.itemView)
                .load(item)
                .into((ImageView) helper.getView(R.id.iv));
        String text = item.getName();
        helper.setText(R.id.tv,text);
    }
}
