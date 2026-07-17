package com.linfeng.music.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalConfigs {

    private static Map<String, Object> localConfigs;

    public static Map<String, Object> getLocalConfigs() {
        if (localConfigs == null) {
            localConfigs = createLocalConfigs();
        }
        return localConfigs;
    }

    private static Map<String, Object> createLocalConfigs() {
        Map<String, Object> configs = new HashMap<>();

        Map<String, String> splashConfig = new HashMap<>();
        splashConfig.put("启动页倒计时", "3");
        splashConfig.put("启动页副标题", "- 音乐搜索 | MusicFree -");
        splashConfig.put("启动页背景图", "http://app.yfxrjk.top/eshare-app/assets/start.jpg");
        splashConfig.put("启动页跳转地址", "http://www.baidu.com");

        configs.put("启动页控制", splashConfig);

        Map<String, String> updateConfig = new HashMap<>();
        updateConfig.put("当前版本", "2.4.0");
        updateConfig.put("弹窗标题", "软件更新");
        updateConfig.put("弹窗内容", "1.新增搜索页面快捷搜索功能。<br>2.修复音乐播放器不同步的问题。<br>3.修复选择音质与下载音质不匹配的问题。");
        updateConfig.put("强制更新", "开启");
        updateConfig.put("更新地址", "https://qwrjk.lanzoub.com/i43r82whfv1e");

        configs.put("软件更新控制", updateConfig);

        List<Map<String, String>> bannerList = new ArrayList<>();
        Map<String, String> banner = new HashMap<>();
        banner.put("标题", "欢迎使用音乐搜索！");
        banner.put("图片地址", "http://app.yfxrjk.top/music-search/images/首页图.jpg");
        banner.put("跳转地址", "https://www.baidu.com/");
        bannerList.add(banner);

        configs.put("首页轮播图", bannerList);

        Map<String, String> miscConfig = new HashMap<>();
        miscConfig.put("反馈Q群", "324372634");

        configs.put("杂项配置", miscConfig);

        return configs;
    }
}
