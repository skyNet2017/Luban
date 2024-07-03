package top.zibin.luban.example;

import android.content.Context;
import android.graphics.Bitmap;

import com.awxkee.avif.glide.AvifCoderByteBufferDecoder;
import com.awxkee.avif.glide.AvifCoderStreamDecoder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @Despciption todo
 * @Author hss
 * @Date 7/3/24 11:16 AM
 * @Version 1.0
 */
//@GlideModule
public class AvifModule2 extends AppGlideModule {

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        // Add the Avif ResourceDecoders before any of the available system decoders. This ensures that
        // the integration will be preferred for Avif images.
        AvifCoderByteBufferDecoder byteBufferBitmapDecoder =
                new AvifCoderByteBufferDecoder(context.getApplicationContext(), glide.getBitmapPool());
        registry.prepend(ByteBuffer.class, Bitmap.class, byteBufferBitmapDecoder);

        AvifCoderStreamDecoder streamBitmapDecoder =
                new AvifCoderStreamDecoder(context.getApplicationContext(), glide.getBitmapPool());
        registry.prepend(InputStream.class, Bitmap.class, streamBitmapDecoder);
    }
}
