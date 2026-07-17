package com.linfeng.music.utils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRequest {
    private static final OkHttpClient client =  new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
    // 静态工厂方法
    public static Builder newRequest() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String method = "GET";
        private final Map<String, String> params = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private RequestBody requestBody;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(String method) {
            this.method = method.toUpperCase();
            return this;
        }

        // 修改：将addParam方法改为直接传递Map集合
        public Builder params(Map<String, String> params) {
            this.params.putAll(params);
            return this;
        }

        // 修改：将addHeader方法改为直接传递Map集合
        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder requestBody(RequestBody body) {
            this.requestBody = body;
            return this;
        }

        public void execute(Callback callback) {
            Request.Builder builder = new Request.Builder();

            // 构建请求URL（处理GET参数）
            if ("GET".equals(method) && !params.isEmpty()) {
                HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
                url = urlBuilder.build().toString();
            }

            // 构建请求体（处理POST参数）
            if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                if (requestBody == null) {
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        formBuilder.add(entry.getKey(), entry.getValue());
                    }
                    requestBody = formBuilder.build();
                }
            }

            // 设置请求头
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }

            // 构建请求
            Request request = builder
                    .url(url)
                    .method(method, requestBody)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body().string());
                    } else {
                        callback.onError(response.code(), response.message());
                    }
                }
            });
        }

    }


    public interface Callback {
        void onSuccess(String response);

        void onError(int statusCode, String message);

        void onFailure(IOException e);
    }
}