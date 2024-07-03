package top.zibin.luban.example;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;



import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.nio.ByteBuffer;

import io.reactivex.annotations.NonNull;
import jp.co.link_u.library.glideavif.AvifDecoderFromByteBuffer;

//https://github.com/link-u/avif-sample-images
@GlideModule
public class AvifModule extends AppGlideModule {

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
        registry.prepend(ByteBuffer.class, Bitmap.class, new AvifDecoderFromByteBuffer());
        Log.w("dd", "glide init.......");
    }

    // Disable manifest parsing to avoid adding similar modules twice.

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
