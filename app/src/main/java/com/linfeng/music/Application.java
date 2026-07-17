package com.linfeng.music;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.arialyy.aria.core.Aria;
import com.bumptech.glide.Glide;
import com.linfeng.music.service.MusicLibraryService;
import com.linfeng.music.utils.ConfigsRepository;
import com.linfeng.music.utils.PreferencesManager;

import java.util.List;
import java.util.Map;

public class Application extends android.app.Application {
    
    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 立即启动音乐服务，让服务在后台先准备好
        Intent serviceIntent = new Intent(this, MusicLibraryService.class);
        startService(serviceIntent);
        
        // 使用广播获取回调
        Aria.get(this).getAppConfig().setUseBroadcast(true);
        
        // 延迟预加载图片，避免影响启动速度
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            preLoadImages();
        }, 2000);
    }

    public static Application getInstance() {
        return instance;
    }

    private void preLoadImages() {
        // 从配置中获取轮播图地址并预加载
        try {
            Map<String, Object> configs = ConfigsRepository.getConfigs();
            if (configs != null && configs.containsKey("首页轮播图")) {
                List<Map<String, String>> bannerList = (List<Map<String, String>>) configs.get("首页轮播图");
                if (bannerList != null) {
                    for (Map<String, String> banner : bannerList) {
                        String imageUrl = banner.get("图片地址");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // 预加载到磁盘缓存
                            Glide.with(this).load(imageUrl).preload();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
