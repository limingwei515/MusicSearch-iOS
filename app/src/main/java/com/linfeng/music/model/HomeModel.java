package com.linfeng.music.model;

import com.linfeng.music.utils.HttpRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

public class HomeModel {
    /**
     * 获取榜单列表的API
     */
    private static final String bangApi = "http://wapi.kuwo.cn/api/pc/bang/list";
    /**
     * 获取热门歌单列表的API
     */
    private static final String songApi = "http://wapi.kuwo.cn/api/pc/classify/playlist/getRcmPlayList";

    /**
     * 获取音乐的歌单信息详情APi
     */
    private static final String MusicSongUrl = "https://mobilebasedata.kuwo.cn/basedata.s";

    /**
     * 获取音乐的榜单列表
     *
     * @return 未解析的数据
     */
    public Single<String> getBangList() {
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(bangApi)
                    .method("GET")
                    .execute(new HttpRequest.Callback() {
                        @Override
                        public void onSuccess(String response) {
                            emitter.onSuccess(response);
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            emitter.onError(new Exception(message));
                        }

                        @Override
                        public void onFailure(IOException e) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    /**
     * 获取热门歌单列表
     *
     * @return 未解析的数据
     */
    public Single<String> getSongList() {
        Map<String, String> params = new HashMap<>();
        params.put("pn", "1");
        params.put("rn", "99");
        params.put("order", "new");
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(songApi)
                    .method("GET")
                    .params(params)
                    .execute(new HttpRequest.Callback() {
                        @Override
                        public void onSuccess(String response) {
                            emitter.onSuccess(response);
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            emitter.onError(new Exception(message));
                        }

                        @Override
                        public void onFailure(IOException e) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    /**
     * 获取指定歌单的歌单信息
     *
     * @param songId 歌单id
     * @return 未解析的数据
     */
    public Single<String> getMusicSongDetail(String songId) {
        Map<String, String> params = new HashMap<>();
        params.put("type", "get_songlist_info2");
        params.put("prod", "0");
        params.put("id", songId);
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(MusicSongUrl)
                    .params(params)
                    .method("GET")
                    .execute(new HttpRequest.Callback() {
                        @Override
                        public void onSuccess(String response) {
                            emitter.onSuccess(response);
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            emitter.onError(new Exception(message));
                        }

                        @Override
                        public void onFailure(IOException e) {
                            emitter.onError(e);
                        }
                    });
        });
    }
}
