package com.linfeng.music.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.linfeng.music.R;

@GlideModule
public class GlideCacheModule extends AppGlideModule {

    private static final long DISK_CACHE_SIZE = 500 * 1024 * 1024; // 500MB
    private static final int MEMORY_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 8); // 内存的1/8

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // 设置内存缓存
        builder.setMemoryCache(new LruResourceCache(MEMORY_CACHE_SIZE));

        // 设置磁盘缓存大小
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, "glide_cache", DISK_CACHE_SIZE));

        // 设置默认占位符
        builder.setDefaultRequestOptions(
                new com.bumptech.glide.request.RequestOptions()
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
        );
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}