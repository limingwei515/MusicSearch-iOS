package com.linfeng.music.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linfeng.music.model.MainModel;
import com.linfeng.music.utils.CommonUtils;
import com.linfeng.music.utils.ConfigsRepository;

import java.lang.reflect.Type;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    /**
     * 加载软件配置文件是否成功
     */
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();

    private final CompositeDisposable disposable = new CompositeDisposable();

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<Boolean> isSuccess() {
        return isSuccess;
    }

    /**
     * 获取软件的基本配置信息
     * <p>
     * 执行model层的网络请求方法，并处理返回数据
     *
     * @param url 配置文件的地址
     * @param key 配置文件的解密密钥
     */
    public void initConfig(String url, String key) {
        Disposable subscribe = new MainModel().getConfig(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            try {
                                // 请求成功 --- 开始解密数据
                                String decryptRes = CommonUtils.decryptAES(data, key);
                                // 数据解密成功 --- 将解密后的数据写入到数据存储库中
                                // 1.使用Gson库处理后台返回的信息
                                Type typeToken = new TypeToken<Map<String, Object>>() {
                                }.getType();
                                // 2.将json信息转换为Map集合
                                Map<String, Object> map = new Gson().fromJson(decryptRes, typeToken);
                                // 3.将配置信息保存到Repository中
                                ConfigsRepository.setConfigs(map);
                                isSuccess.postValue(true);
                            } catch (Exception e) {
                                isSuccess.postValue(false);
                                Log.e(TAG, "解析配置文档失败！", e);
                            }
                        },
                        error -> {
                            isSuccess.postValue(false);
                            Log.e(TAG, "获取应用配置信息失败！", error);
                        }
                );
        disposable.add(subscribe);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.clear();
    }
}
