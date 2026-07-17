package com.linfeng.music.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.linfeng.music.R;

public class ImageLoader {

    public static void loadImage(Context context, String url, ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(url)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }

    public static void loadImage(Context context, String url, ImageView imageView, int placeholderRes) {
        RequestOptions options = new RequestOptions()
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(url)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }

    public static void loadImageWithoutPlaceholder(Context context, String url, ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .error(R.mipmap.ic_launcher)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(url)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }

    public static void loadImageWithBlur(Context context, String url, ImageView imageView, int radius, int sampling) {
        RequestOptions options = new RequestOptions()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(url)
                .apply(options)
                .transform(new jp.wasabeef.glide.transformations.BlurTransformation(radius, sampling))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }

    public static void preloadImage(Context context, String url) {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false);
        Glide.with(context)
                .load(url)
                .apply(options)
                .preload();
    }

    public static void preloadImage(Context context, String url, int width, int height) {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .override(width, height);
        Glide.with(context)
                .load(url)
                .apply(options)
                .preload(width, height);
    }

    public static void clearDiskCache(Context context) {
        new Thread(() -> {
            Glide.get(context.getApplicationContext()).clearDiskCache();
        }).start();
    }

    public static void clearMemoryCache(Context context) {
        Glide.get(context.getApplicationContext()).clearMemory();
    }

    public static void clearAllCache(Context context) {
        clearDiskCache(context);
        clearMemoryCache(context);
    }
}