package com.linfeng.music.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PreferencesManager {
    private static final String PREF_NAME = "userInfo";
    private static PreferencesManager instance;
    private SharedPreferences sharedPreferences;
    private static final Gson gson = new Gson();

    private PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    public void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void putInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int value) {
        return sharedPreferences.getInt(key, value);
    }

    public void putLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public long getLong(String key, long value) {
        return sharedPreferences.getLong(key, value);
    }

    public void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public <T> void putList(Context context, String key, List<T> list) {
        String json = gson.toJson(list);
        putString(key, json);
    }

    public <T> List<T> getList(Context context, String key, Class<T> clazz) {
        String json = getString(key, null);
        if (json == null) {
            // 没有数据直接返回空值
            return null;
        }
        Type type = TypeToken.getParameterized(List.class, clazz).getType();
        return gson.fromJson(json, type);
    }

    public boolean containsKey(String key) {
        return sharedPreferences.contains(key);
    }

    public void removeAll() {
        sharedPreferences.edit().clear().apply();
    }
}
