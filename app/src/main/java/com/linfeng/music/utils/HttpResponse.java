package com.linfeng.music.utils;

/**
 * 请求结果返回封装类
 *
 * @author zhaosir
 */

public class HttpResponse {
    public boolean success;
    public String error;
    public String data;

    public HttpResponse(boolean success, String error, String data) {
        this.success = success;
        this.error = error;
        this.data = data;
    }

    public static HttpResponse success(String data) {
        return new HttpResponse(true, null, data);
    }

    public static HttpResponse error(String error) {
        return new HttpResponse(false, error, null);
    }
}
