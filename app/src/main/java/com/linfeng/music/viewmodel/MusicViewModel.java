package com.linfeng.music.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.linfeng.music.bean.MusicBean;
import com.linfeng.music.model.MusicModel;
import com.linfeng.music.repository.FavoriteRepository;
import com.linfeng.music.utils.MySubString;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MusicViewModel extends AndroidViewModel {
    /**
     * 榜单歌曲的List集合数据
     */
    private final MutableLiveData<List<MusicBean>> bangMusicData = new MutableLiveData<>();
    /**
     * 歌单的List集合数据
     */
    private final MutableLiveData<List<MusicBean>> playListMusicData = new MutableLiveData<>();
    /**
     * 搜索音乐的List集合数据
     */
    private final MutableLiveData<List<MusicBean>> searchMusicData = new MutableLiveData<>();
    /**
     * 喜欢音乐的List集合数据
     */
    private final MutableLiveData<List<MusicBean>> likeMusicData = new MutableLiveData<>();
    /**
     * 音乐的音质集合
     */
    private final MutableLiveData<List<Map<String, String>>> musicTonalList = new MutableLiveData<>();
    /**
     * 音乐的下载地址（指定音质）
     */
    private final MutableLiveData<String> musicDownloadUrl = new MutableLiveData<>();
    /**
     * 是否还有更多数据
     */
    private final MutableLiveData<Boolean> hasLoadingMoreData = new MutableLiveData<>();
    /**
     * 数据是否加载成功 --- 仅检查第一页数据的加载
     */
    private final MutableLiveData<Boolean> isLoadingSuccess = new MutableLiveData<>();

    private final CompositeDisposable disposable = new CompositeDisposable();
    private final FavoriteRepository favoriteRepository;

    public MusicViewModel(@NonNull Application application) {
        super(application);
        favoriteRepository = FavoriteRepository.getInstance(application);
    }


    public LiveData<Boolean> getHasLoadingMoreData() {
        return hasLoadingMoreData;
    }

    public LiveData<List<MusicBean>> getBangMusicData() {
        return bangMusicData;
    }

    public LiveData<List<MusicBean>> getPlayListMusicData() {
        return playListMusicData;
    }

    public LiveData<List<MusicBean>> getSearchMusicData() {
        return searchMusicData;
    }

    public LiveData<List<MusicBean>> getLikeMusicData() {
        return likeMusicData;
    }

    public LiveData<List<Map<String, String>>> getMusicTonalList() {
        return musicTonalList;
    }

    public LiveData<String> getMusicDownloadUrl() {
        return musicDownloadUrl;
    }

    public LiveData<Boolean> getIsLoadingSuccess() {
        return isLoadingSuccess;
    }

    public void initBangMusicData(String id, int page, String size) {
        Disposable subscribe = new MusicModel().getBangMusic(id, page, size)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            // 这里是解析数据的逻辑
                            try {
                                JSONObject jsonObject = new JSONObject(data);
                                JSONArray musiclist = jsonObject.getJSONArray("musiclist");
                                List<MusicBean> temp = new ArrayList<>();
                                for (int i = 0; i < musiclist.length(); i++) {
                                    MusicBean musicBean = new MusicBean();
                                    JSONObject jsonObject1 = musiclist.getJSONObject(i);
                                    musicBean.setId(jsonObject1.getString("id"));
                                    musicBean.setName(jsonObject1.getString("name"));
                                    Log.e("MusicViewModel", jsonObject1.getString("name"));
                                    musicBean.setArtist(jsonObject1.getString("artist"));
                                    // 修复图片显示异常，增强字段兼容性，自动补全URL
                                    String albumIcon = "";
                                    if (jsonObject1.has("img") && !jsonObject1.isNull("img") && jsonObject1.getString("img").length() > 5) {
                                        albumIcon = jsonObject1.getString("img");
                                    } else if (jsonObject1.has("pic") && !jsonObject1.isNull("pic") && jsonObject1.getString("pic").length() > 5) {
                                        albumIcon = jsonObject1.getString("pic");
                                    } else if (jsonObject1.has("albumpic") && !jsonObject1.isNull("albumpic") && jsonObject1.getString("albumpic").length() > 5) {
                                        albumIcon = jsonObject1.getString("albumpic");
                                    } else if (jsonObject1.has("album_img") && !jsonObject1.isNull("album_img") && jsonObject1.getString("album_img").length() > 5) {
                                        albumIcon = jsonObject1.getString("album_img");
                                    } else if (jsonObject1.has("album_pic") && !jsonObject1.isNull("album_pic") && jsonObject1.getString("album_pic").length() > 5) {
                                        albumIcon = jsonObject1.getString("album_pic");
                                    }
                                    // 自动补全URL前缀
                                    if (!albumIcon.startsWith("http") && albumIcon.length() > 5) {
                                        albumIcon = "https://img4.kuwo.cn/star/albumcover/" + albumIcon;
                                    }
                                    // 如果还是没有图片，给默认图片
                                    if (albumIcon.isEmpty() || albumIcon.length() < 5) {
                                        albumIcon = "https://img1.kuwo.cn/star/albumcover/default.jpg";
                                    }
                                    musicBean.setAlbumIcon(albumIcon);
                                    temp.add(musicBean);
                                }
                                if (page == 0) {
                                    bangMusicData.postValue(temp);
                                } else {
                                    if (temp.isEmpty()) {
                                        hasLoadingMoreData.postValue(false);
                                        return;
                                    }
                                    List<MusicBean> nowData = bangMusicData.getValue();
                                    nowData.addAll(temp);
                                    bangMusicData.postValue(nowData);
                                }
                            } catch (Exception e) {
                                if (page == 0) {// 第一页解析失败直接弹错误
                                    isLoadingSuccess.postValue(false);
                                } else {
                                    hasLoadingMoreData.postValue(false);
                                }
                                Log.e("MusicViewModel", "解析数据失败！", e);
                            }
                        },
                        error -> {
                            if (page == 0) {// 第一页解析失败直接弹错误
                                isLoadingSuccess.postValue(false);
                            } else {
                                hasLoadingMoreData.postValue(false);
                            }
                            Log.e("MusicViewModel", "获取音乐列表数据失败！", error);
                        });
        disposable.add(subscribe);
    }

    public void initPlayListMusicData(String id, int page, String size) {
        Disposable subscribe = new MusicModel().getPlayListMusic(id, page, size)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            try {
                                JSONObject jsonObject = new JSONObject(data);
                                JSONObject data1 = jsonObject.getJSONObject("data");
                                JSONArray musiclist = data1.getJSONArray("musiclist");
                                List<MusicBean> temp = new ArrayList<>();
                                for (int i = 0; i < musiclist.length(); i++) {
                                    MusicBean musicBean = new MusicBean();
                                    JSONObject jsonObject1 = musiclist.getJSONObject(i);
                                    musicBean.setName(jsonObject1.getString("name"));
                                    musicBean.setArtist(jsonObject1.getString("artist"));
                                    musicBean.setId(jsonObject1.getString("rid"));
                                    musicBean.setAlbumIcon(jsonObject1.getString("img"));
                                    temp.add(musicBean);
                                }
                                if (page == 0) {
                                    playListMusicData.postValue(temp);
                                } else {
                                    if (temp.isEmpty()) {
                                        hasLoadingMoreData.postValue(false);
                                        return;
                                    }
                                    List<MusicBean> nowData = playListMusicData.getValue();
                                    nowData.addAll(temp);
                                    playListMusicData.postValue(nowData);
                                }
                            } catch (Exception e) {
                                if (page == 0) {// 第一页解析失败直接弹错误
                                    isLoadingSuccess.postValue(false);
                                } else {
                                    hasLoadingMoreData.postValue(false);
                                }
                                Log.e("MusicViewModel", "解析数据失败！", e);
                            }
                        },
                        error -> {
                            if (page == 0) {// 第一页解析失败直接弹错误
                                isLoadingSuccess.postValue(false);
                            } else {
                                hasLoadingMoreData.postValue(false);
                            }
                            Log.e("MusicViewModel", "获取音乐列表数据失败！", error);
                        });
        disposable.add(subscribe);
    }

    public void initSearchMusicData(String name, int page, String size) {
        Disposable subscribe = new MusicModel().getSearchMusic(name, page, size)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            try {
                                List<MusicBean> temp = new ArrayList<>();
                                JSONObject jsonObject = new JSONObject(data);
                                JSONArray abslist = jsonObject.getJSONArray("abslist");
                                for (int i = 0; i < abslist.length(); i++) {
                                    MusicBean musicBean = new MusicBean();
                                    JSONObject jsonObject1 = abslist.getJSONObject(i);
                                    musicBean.setArtist(jsonObject1.getString("ARTIST"));
                                    musicBean.setName(jsonObject1.getString("NAME"));
                                    musicBean.setId(jsonObject1.getString("DC_TARGETID"));
                                    musicBean.setAlbumIcon(String.format("https://img4.kuwo.cn/star/albumcover/%s", jsonObject1.getString("web_albumpic_short")));
                                    musicBean.setLike(false);
                                    temp.add(musicBean);
                                }
                                if (page == 0) {
                                    searchMusicData.postValue(temp);
                                } else {
                                    if (temp.isEmpty()) {
                                        hasLoadingMoreData.postValue(false);
                                        return;
                                    }
                                    List<MusicBean> nowData = searchMusicData.getValue();
                                    nowData.addAll(temp);
                                    searchMusicData.postValue(nowData);
                                }
                            } catch (Exception e) {
                                if (page == 0) {// 第一页解析失败直接弹错误
                                    isLoadingSuccess.postValue(false);
                                } else {
                                    hasLoadingMoreData.postValue(false);
                                }
                                Log.e("MusicViewModel", "解析数据失败！", e);
                            }
                        },
                        error -> {
                            if (page == 0) {// 第一页解析失败直接弹错误
                                isLoadingSuccess.postValue(false);
                            } else {
                                hasLoadingMoreData.postValue(false);
                            }
                            Log.e("MusicViewModel", "获取音乐列表数据失败！", error);
                        });
        disposable.add(subscribe);
    }

    public void initLikeMusicData() {
        disposable.add(
            favoriteRepository.getAllFavoritesAsMusicBeans()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    likeMusicData::postValue,
                    error -> {
                        Log.e("MusicViewModel", "查询收藏失败", error);
                        likeMusicData.postValue(new ArrayList<>());
                    }
                )
        );
    }

    public void getMusicDetail(String musicId) {
        Disposable subscribe = new MusicModel().getMusicDetail(musicId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(data -> {
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        JSONArray songs = jsonObject.getJSONArray("songs");
                        JSONObject jsonObject1 = songs.getJSONObject(0);
                        String minfo = jsonObject1.getString("MINFO");
                        String[] split = minfo.split(";");
                        List<Map<String, String>> musicTonal = new ArrayList<>();
                        for (String s : split) {
                            Map<String, String> map = new HashMap<>();
                            if (s.contains("bitrate:2000")) {
                                map.put("2000", MySubString.subStartString(s, "size:"));
                                musicTonal.add(map);
                            }

                            if (s.contains("bitrate:320")) {
                                map.put("320", MySubString.subStartString(s, "size:"));
                                musicTonal.add(map);
                            }

                            if (s.contains("bitrate:128")) {
                                map.put("128", MySubString.subStartString(s, "size:"));
                                musicTonal.add(map);
                            }

                            if (s.contains("bitrate:48")) {
                                map.put("48", MySubString.subStartString(s, "size:"));
                                musicTonal.add(map);
                            }
                        }
                        musicTonal.sort((map1, map2) -> {
                            // 假设每个Map只有一个key，获取第一个key
                            String key1 = map1.keySet().iterator().next();
                            String key2 = map2.keySet().iterator().next();

                            // 转换为数字并比较（降序排列）
                            return Integer.compare(Integer.parseInt(key2), Integer.parseInt(key1));
                        });
                        musicTonalList.postValue(musicTonal);
                    } catch (Exception e) {
                        Log.e("MusicViewModel", "获取音乐音质失败！", e);
                    }
                }, error -> {
                    Log.e("MusicViewModel", "获取音乐音质失败！", error);
                });
        disposable.add(subscribe);
    }

    public void getMusicDownloadUrl(String musicId, String bitrate) {
        Disposable subscribe = new MusicModel().getMusicDownloadUrl(musicId, bitrate)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(data -> {
                    String downloadUrl = MySubString.subString(data, "url=", "sig");
                    if (downloadUrl != null) {
                        musicDownloadUrl.postValue(downloadUrl);
                    } else {
                        Log.e("MusicViewModel", "获取音乐下载地址失败！");
                    }
                }, error -> {
                    Log.e("MusicViewModel", "获取音乐下载地址失败！", error);
                });
        disposable.add(subscribe);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.clear();
    }
}
