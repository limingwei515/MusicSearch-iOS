package com.linfeng.music.model;

import com.linfeng.music.utils.HttpRequest;

import java.io.IOException;

import io.reactivex.rxjava3.core.Single;

public class MainModel {

    /**
     * 请求配置信息
     *
     * @param url 请求地址
     * @return 响应数据
     */
    public Single<String> getConfig(String url) {
        return Single.create(emitter -> {
            HttpRequest.newRequest()
                    .url(url)
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
