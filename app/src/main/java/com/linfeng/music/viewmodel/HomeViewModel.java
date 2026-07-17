package com.linfeng.music.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.linfeng.music.bean.BangBean;
import com.linfeng.music.bean.SongBean;
import com.linfeng.music.model.HomeModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeViewModel extends AndroidViewModel {

    private final CompositeDisposable disposable = new CompositeDisposable();
    /**
     * 榜单列表数据
     */
    private static final MutableLiveData<List<BangBean>> bangList = new MutableLiveData<>();
    /**
     * 推荐歌单列表数据
     */
    private static final MutableLiveData<List<SongBean>> songList = new MutableLiveData<>();
    /**
     * 指定id的歌单详情 --- 用作解析用户绑定的歌单
     */
    private final MutableLiveData<SongBean> songDetail = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<BangBean>> getBangList() {
        return bangList;
    }

    public LiveData<List<SongBean>> getSongList() {
        return songList;
    }

    public LiveData<SongBean> getSongDetail() {
        return songDetail;
    }

    public void initBangConfig() {
        Disposable subscribe = new HomeModel().getBangList()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(data -> {
                            // 请求成功 --- 开始解析数据到bean
                            try {
                                JSONObject jsonObject = new JSONObject(data);
                                JSONArray child = jsonObject.getJSONArray("child");
                                JSONObject jsonObject1 = child.getJSONObject(0);
                                JSONArray child1 = jsonObject1.getJSONArray("child");
                                List<BangBean> tempList = new ArrayList<>();
                                for (int i = 0; i < child1.length(); i++) {
                                    BangBean bangBean = new BangBean();
                                    JSONObject jsonObject2 = child1.getJSONObject(i);
                                    String name = jsonObject2.getString("name");
                                    String id = jsonObject2.getString("sourceid");
                                    String picUrl = jsonObject2.getString("pic2");
                                    bangBean.setBangName(name);
                                    bangBean.setBangId(id);
                                    bangBean.setBangImgUrl(picUrl);
                                    tempList.add(bangBean);
                                }
                                bangList.postValue(tempList);
                            } catch (Exception e) {
                                Log.e("HomeViewModel", "解析榜单列表数据失败", e);
                                bangList.postValue(new ArrayList<>());
                            }
                        },
                        error -> {
                            // 处理错误
                            Log.e("HomeViewModel", "解析榜单列表数据失败", error);
                            bangList.postValue(new ArrayList<>());
                        });
        disposable.add(subscribe);
    }

    public void initSongConfig() {
        Disposable subscribe = new HomeModel().getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(data -> {
                            // 请求成功 --- 开始解析数据到bean
                            try {
                                JSONObject jsonObject = new JSONObject(data);
                                JSONObject data1 = jsonObject.getJSONObject("data");
                                JSONArray data2 = data1.getJSONArray("data");
                                List<SongBean> tempList = new ArrayList<>();
                                for (int i = 0; i < data2.length(); i++) {
                                    SongBean songBean = new SongBean();
                                    JSONObject jsonObject2 = data2.getJSONObject(i);
                                    String name = jsonObject2.getString("name");
                                    String id = jsonObject2.getString("id");
                                    String picUrl = jsonObject2.getString("img");
                                    String listencnt = jsonObject2.getString("listencnt");
                                    songBean.setSongName(name);
                                    songBean.setSongImgUrl(picUrl);
                                    songBean.setSongId(id);
                                    songBean.setListencnt(listencnt);
                                    tempList.add(songBean);
                                }
                                songList.postValue(tempList);
                            } catch (Exception e) {
                                Log.e("HomeViewModel", "解析榜单列表数据失败", e);
                                songList.postValue(new ArrayList<>());
                            }
                        },
                        error -> {
                            // 处理错误
                            Log.e("HomeViewModel", "解析榜单列表数据失败", error);
                            songList.postValue(new ArrayList<>());
                        });
        disposable.add(subscribe);
    }

    public void getSongDetail(String songId) {
        Disposable subscribe = new HomeModel().getMusicSongDetail(songId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(data -> {
                    // 请求成功 --- 开始解析数据到bean
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        JSONObject info = jsonObject.getJSONObject("sl_data");
                        SongBean songBean = new SongBean();
                        songBean.setSongName(info.getString("title"));
                        songBean.setSongImgUrl(info.getString("big_pic"));
                        songBean.setListencnt(info.getString("play_num"));
                        songBean.setSongId(songId);
                        songDetail.postValue(songBean);
                    } catch (Exception e) {
                        SongBean songBean = new SongBean();
                        songBean.setSongId("error");
                        songDetail.postValue(songBean);
                        Log.e("HomeViewModel", "解析歌单详情数据失败", e);
                    }
                }, error -> {
                    SongBean songBean = new SongBean();
                    songBean.setSongId("error");
                    songDetail.postValue(songBean);
                    // 处理错误
                    Log.e("HomeViewModel", "获取歌单详情数据失败", error);
                });
        disposable.add(subscribe);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.clear(); // 及时清理订阅
    }
}
