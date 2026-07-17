package com.linfeng.music.model;

import com.linfeng.music.utils.EncryptMusicToken;
import com.linfeng.music.utils.HttpRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

public class MusicModel {
    /**
     * 获取榜单音乐详情的API
     */
    private static final String BangMusicUrl = "http://kbangserver.kuwo.cn/ksong.s";
    /**
     * 获取歌单音乐详情的API
     */
    private static final String PlayListUrl = "https://mobilist.kuwo.cn/list.s";
    /**
     * 搜索音乐详情的API
     */
    private static final String SearchUrl = "https://search.kuwo.cn/r.s";
    /**
     * 获取音乐url的API
     */
    private static final String MusicPlayUrl = "http://nmobi.kuwo.cn/mobi.s?f=kuwo&q=";
    /**
     * 获取音乐歌词的API
     */
    private static final String MusicLyricUrl = "http://m.kuwo.cn/newh5/singles/songinfoandlrc";
    /**
     * 获取音乐的支持音质详情
     */
    private static final String MusicDetailUrl = "https://musicpay.kuwo.cn/music.pay";



    /**
     * 获取榜单音乐的List数据
     *
     * @param id   榜单id
     * @param page 页数
     * @param size 长度
     * @return 未解析的数据
     */
    public Single<String> getBangMusic(String id, int page, String size) {
        // 设置请求参数
        Map<String, String> params = new HashMap<>();
        params.put("from", "pc");
        params.put("type", "bang");
        params.put("id", id);
        params.put("pn", String.valueOf(page));
        params.put("rn", size);
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(BangMusicUrl)
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


    /**
     * 获取歌单的音乐详情
     *
     * @param id   歌单id
     * @param page 页数
     * @param size 长度
     * @return 未解析的数据
     */
    public Single<String> getPlayListMusic(String id, int page, String size) {
        Map<String, String> params = new HashMap<>();
        params.put("type", "songlist");
        params.put("id", id);
        params.put("pn", String.valueOf(page));
        params.put("rn", size);
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(PlayListUrl)
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

    /**
     * 获取搜索的音乐详情
     *
     * @param name 歌曲名称
     * @param page 页数
     * @param size 长度
     * @return 未解析的数据
     */
    public Single<String> getSearchMusic(String name, int page, String size) {
        Map<String, String> params = new HashMap<>();
        params.put("client", "kt");
        params.put("all", name);
        params.put("vipver", "1");
        params.put("ft", "music");
        params.put("encoding", "utf8");
        params.put("rformat", "json");
        params.put("mobi", "1");
        params.put("pn", String.valueOf(page));
        params.put("rn", size);
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(SearchUrl)
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


    /**
     * 获取指定音乐id的播放地址（带音质参数）
     * @param musicId 音乐id
     * @param bitrate 音质码率（如2000、320、128、48）
     * @return 未解析的数据
     */
    public Single<String> getMusicPlayInfo(String musicId, String bitrate) {
        String api = "user=0&android_id=0&prod=kwplayerhd_ar_4.3.0.8&corp=kuwo&vipver=4.3.0.8&source=kwplayerhd_ar_4.3.0.8_tianbao_T1A_qirui.apk&notrace=0&type=convert_url2&br=" + bitrate + "&format=flac|mp3|aac&sig=0&priority=bitrate&loginUid=0&network=WIFI&loginSid=0&mode=down&rid=" + musicId;
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(MusicPlayUrl + EncryptMusicToken.encrypt(api))
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
     * 获取指定音乐的歌词
     *
     * @param musicId 音乐id
     * @return 未解析的数据
     */
    public Single<String> getMusicLyric(String musicId) {
        Map<String, String> params = new HashMap<>();
        params.put("musicId", musicId);
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(MusicLyricUrl)
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

    /**
     * 获取指定音乐的详情 --- 这里主要是获取歌曲音质
     *
     * @param musicId 歌曲id
     * @return 未解析的数据
     */
    public Single<String> getMusicDetail(String musicId) {
        Map<String, String> params = new HashMap<>();
        params.put("op", "query");
        params.put("action", "play");
        params.put("ids", musicId);
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(MusicDetailUrl)
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

    /**
     * 获取指定音乐id和音质的下载地址
     *
     * @param musicId 音乐id
     * @param bitrate 音质
     * @return 未解析的数据
     */
    public Single<String> getMusicDownloadUrl(String musicId, String bitrate) {
        String api = "user=0&android_id=0&prod=kwplayerhd_ar_4.3.0.8&corp=kuwo&vipver=4.3.0.8&source=kwplayerhd_ar_4.3.0.8_tianbao_T1A_qirui.apk&p2p=1&notrace=0&type=convert_url2&br=" + bitrate + "&format=flac|mp3|aac&rid=" + musicId + "&priority=bitrate&loginUid=0&network=WIFI&loginSid=0&mode=down";
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(MusicPlayUrl + EncryptMusicToken.encrypt(api))
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
