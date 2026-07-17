package com.linfeng.music.viewmodel;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dirror.lyricviewx.LyricEntry;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linfeng.music.bean.MusicBean;
import com.linfeng.music.bean.PlayingMode;
import com.linfeng.music.database.AppDatabase;
import com.linfeng.music.database.dao.FavoriteDao;
import com.linfeng.music.database.dao.LyricCacheDao;
import com.linfeng.music.database.entity.FavoriteEntity;
import com.linfeng.music.database.entity.LyricCacheEntity;
import com.linfeng.music.model.MusicModel;
import com.linfeng.music.repository.FavoriteRepository;
import com.linfeng.music.repository.LyricCacheRepository;
import com.linfeng.music.service.MusicLibraryService;
import com.linfeng.music.utils.MySubString;
import com.linfeng.music.utils.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MusicPlayerViewModel extends AndroidViewModel {

    private static final String TAG = "MusicPlayerViewModel";
    private static final String KEY_LAST_MUSIC_ID = "last_played_music_id";
    private static final String KEY_LAST_MUSIC_NAME = "last_played_music_name";
    private static final String KEY_LAST_MUSIC_ARTIST = "last_played_music_artist";
    private static final String KEY_LAST_MUSIC_ALBUM = "last_played_music_album";
    private static final String KEY_LAST_POSITION = "last_played_position";
    private static final String KEY_LAST_DURATION = "last_played_duration";
    private static final String KEY_PLAYLIST = "playlist_cache";
    private static final String KEY_PLAYING_MODE = "playing_mode";

    private MusicLibraryService musicService;

    // ========== LiveData ==========
    private static final MutableLiveData<List<MusicBean>> nowPlayingMusicList = new MutableLiveData<>();
    private static final MutableLiveData<Integer> nowPlayingPosition = new MutableLiveData<>();
    private static final MutableLiveData<MusicBean> nowPlayingMusicData = new MutableLiveData<>();
    private static final MutableLiveData<Integer> nowMusicPlayingProgress = new MutableLiveData<>();
    private static final MutableLiveData<Integer> nowMusicPlayingDuration = new MutableLiveData<>();
    private static final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();
    private static final MutableLiveData<PlayingMode> nowPlayingMode = new MutableLiveData<>(PlayingMode.SHUNXU);
    private static final MutableLiveData<List<LyricEntry>> lyricList = new MutableLiveData<>();
    private static final MutableLiveData<Boolean> isLiked = new MutableLiveData<>();
    private static final MutableLiveData<Boolean> isLyricLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Map<String, String>>> musicTonalList = new MutableLiveData<>();
    private final MutableLiveData<String> musicDownloadUrl = new MutableLiveData<>();

    private boolean isGettingMusicDetail = false;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private Disposable lyricDisposable = null;
    private final Gson gson = new Gson();

    // 播放状态标记
    private boolean hasRestoredState = false;
    private boolean hasPlayedOnce = false;

    // Room 数据库访问
    private FavoriteDao favoriteDao;
    private LyricCacheDao lyricCacheDao;
    // Repository 层
    private FavoriteRepository favoriteRepository;
    private LyricCacheRepository lyricCacheRepository;

    public MusicPlayerViewModel(@NonNull Application application) {
        super(application);
        // 初始化 Repository
        favoriteRepository = FavoriteRepository.getInstance(application);
        lyricCacheRepository = LyricCacheRepository.getInstance(application);
        favoriteDao = AppDatabase.getInstance(application).favoriteDao();
        lyricCacheDao = AppDatabase.getInstance(application).lyricCacheDao();
        // 尝试恢复播放状态
        restorePlayState();
    }

    // ==============================================================
    //  持久化：保存
    // ==============================================================
    public void savePlayState() {
        PreferencesManager prefs = PreferencesManager.getInstance(getApplication());
        MusicBean currentMusic = nowPlayingMusicData.getValue();
        if (currentMusic != null) {
            prefs.putString(KEY_LAST_MUSIC_ID, currentMusic.getId());
            prefs.putString(KEY_LAST_MUSIC_NAME, currentMusic.getName());
            prefs.putString(KEY_LAST_MUSIC_ARTIST, currentMusic.getArtist());
            prefs.putString(KEY_LAST_MUSIC_ALBUM, currentMusic.getAlbumIcon() == null ? "" : currentMusic.getAlbumIcon());
            prefs.putInt(KEY_LAST_POSITION, nowPlayingPosition.getValue() != null ? nowPlayingPosition.getValue() : 0);
            prefs.putInt(KEY_LAST_DURATION, nowMusicPlayingDuration.getValue() != null ? nowMusicPlayingDuration.getValue() : 0);
        }
        List<MusicBean> list = nowPlayingMusicList.getValue();
        if (list != null && !list.isEmpty()) {
            prefs.putString(KEY_PLAYLIST, gson.toJson(list));
        }
        PlayingMode mode = nowPlayingMode.getValue();
        if (mode != null) {
            prefs.putString(KEY_PLAYING_MODE, mode.name());
        }
        Log.d(TAG, "已保存播放状态");
    }

    // ==============================================================
    //  持久化：恢复
    // ==============================================================
    public boolean restorePlayState() {
        PreferencesManager prefs = PreferencesManager.getInstance(getApplication());
        String musicId = prefs.getString(KEY_LAST_MUSIC_ID, "");
        if (musicId.isEmpty()) return false;

        MusicBean bean = new MusicBean();
        bean.setId(musicId);
        bean.setName(prefs.getString(KEY_LAST_MUSIC_NAME, ""));
        bean.setArtist(prefs.getString(KEY_LAST_MUSIC_ARTIST, ""));
        String album = prefs.getString(KEY_LAST_MUSIC_ALBUM, "");
        bean.setAlbumIcon(album.isEmpty() ? null : album);

        nowPlayingMusicData.postValue(bean);
        nowPlayingPosition.postValue(prefs.getInt(KEY_LAST_POSITION, 0));
        isPlaying.postValue(false);

        // 恢复播放列表
        String playlistJson = prefs.getString(KEY_PLAYLIST, null);
        if (playlistJson != null) {
            try {
                Type type = new TypeToken<List<MusicBean>>() {}.getType();
                List<MusicBean> list = gson.fromJson(playlistJson, type);
                if (list != null && !list.isEmpty()) {
                    nowPlayingMusicList.postValue(list);
                }
            } catch (Exception ignored) {}
        } else {
            List<MusicBean> single = new ArrayList<>();
            single.add(bean);
            nowPlayingMusicList.postValue(single);
        }

        String modeStr = prefs.getString(KEY_PLAYING_MODE, PlayingMode.SHUNXU.name());
        try {
            nowPlayingMode.postValue(PlayingMode.valueOf(modeStr));
        } catch (Exception ignored) {}

        initMusicLyric(musicId);
        checkIfLiked(musicId);
        hasRestoredState = true;
        Log.d(TAG, "恢复播放状态: " + bean.getName());
        return true;
    }

    // ==============================================================
    //  LiveData 对外只读接口
    // ==============================================================
    public LiveData<MusicBean> getNowPlayingMusicData() { return nowPlayingMusicData; }
    public LiveData<Integer> getNowMusicPlayingProgress() { return nowMusicPlayingProgress; }
    public LiveData<Integer> getNowMusicPlayingDuration() { return nowMusicPlayingDuration; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<List<LyricEntry>> getLyricList() { return lyricList; }
    public LiveData<Boolean> getIsLyricLoading() { return isLyricLoading; }
    public LiveData<List<Map<String, String>>> getMusicTonalList() { return musicTonalList; }
    public LiveData<String> getMusicDownloadUrl() { return musicDownloadUrl; }
    public LiveData<Boolean> getIsLiked() { return isLiked; }

    public MusicBean getNowPlayingMusicDataValue() { return nowPlayingMusicData.getValue(); }
    public boolean hasRestoredState() { return hasRestoredState; }

    // ==============================================================
    //  服务绑定
    // ==============================================================
    public void initMusicPlayService(MusicLibraryService service) {
        this.musicService = service;
        // 播放状态回调
        musicService.setPlaybackListener(new MusicLibraryService.PlaybackListener() {
            @Override public void onPlaybackStateChanged(int state) {}
            @Override public void onIsPlayingChanged(boolean playing) {
                isPlaying.postValue(playing);
            }
            @Override public void onPlaybackCompleted() {} // 由 SkipCallback 处理
            @Override public void onPlayerError(String msg) {
                Log.e(TAG, "播放错误: " + msg);
                tryLowerQuality();
            }
        });
        // 进度回调
        musicService.setProgressCallback((current, duration) -> {
            nowMusicPlayingProgress.postValue(current / 1000);
            nowMusicPlayingDuration.postValue(duration / 1000);
        });
        // 切歌回调（自动切歌 + 通知栏切歌 + 蓝牙切歌）
        musicService.setSkipCallback((nextIndex, nextSong) -> {
            nowPlayingPosition.postValue(nextIndex);
            nowPlayingMusicData.postValue(nextSong);
            playMusic(nextSong.getId(), nextIndex, service.getPlaybackQueue());
        });
    }

    // ==============================================================
    //  核心播放
    // ==============================================================
    public void playMusic(@Nullable String musicId, Integer position, @Nullable List<MusicBean> musicList) {
        if (musicService == null) return;

        final List<MusicBean> finalList;
        final int finalPos;

        if (musicId != null && musicList != null) {
            finalList = musicList;
            finalPos = position;
            nowPlayingMusicList.postValue(musicList);
            nowPlayingMusicData.postValue(musicList.get(position));
            nowPlayingPosition.postValue(position);
            checkIfLiked(musicId);
        } else if (musicList != null && nowPlayingMusicList.getValue() != null) {
            finalList = nowPlayingMusicList.getValue();
            finalPos = position;
            MusicBean bean = finalList.get(position);
            nowPlayingMusicData.postValue(bean);
        } else {
            MusicBean current = nowPlayingMusicData.getValue();
            if (current == null) return;
            finalList = new ArrayList<>();
            finalList.add(current);
            finalPos = 0;
        }

        lyricList.postValue(new ArrayList<>());
        isLyricLoading.postValue(true);

        // 选择音质
        boolean isCellular = isOnCellularNetwork();
        boolean cellularAutoLower = PreferencesManager.getInstance(getApplication()).getBoolean("cellular_auto_lower", true);
        String quality = isCellular && cellularAutoLower
                ? PreferencesManager.getInstance(getApplication()).getString("cellular_play_quality", "standard")
                : PreferencesManager.getInstance(getApplication()).getString("play_quality", "lossless");
        String bitrate = qualityToBitrate(quality);

        String targetId = finalList.get(finalPos).getId();
        Disposable subscribe = new MusicModel()
                .getMusicPlayInfo(targetId, bitrate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            String musicUrl = MySubString.subString(data, "url=", "sig");
                            MusicBean bean = nowPlayingMusicData.getValue();
                            musicService.play(musicUrl, bean, finalList, finalPos);
                            hasPlayedOnce = true;
                            isPlaying.postValue(true);
                            initMusicLyric(targetId);
                            savePlayState();
                        },
                        error -> {
                            Log.e(TAG, "播放请求失败: " + error.getMessage());
                            tryLowerQuality();
                        });
        disposable.add(subscribe);
    }

    private String qualityToBitrate(String quality) {
        switch (quality) {
            case "lossless": return "2000";
            case "high": return "320";
            case "standard": return "128";
            default: return "48";
        }
    }

    // ==============================================================
    //  播放控制
    // ==============================================================
    public void toggleMusic() {
        if (musicService == null) return;
        MusicBean currentMusic = nowPlayingMusicData.getValue();
        if (currentMusic == null) return;

        if (!hasPlayedOnce) {
            List<MusicBean> list = nowPlayingMusicList.getValue();
            if (list == null || list.isEmpty()) {
                list = new ArrayList<>();
                list.add(currentMusic);
            }
            playMusic(currentMusic.getId(), 0, list);
            return;
        }

        if (Boolean.TRUE.equals(isPlaying.getValue())) {
            musicService.pause();
            isPlaying.setValue(false);
        } else {
            musicService.resume();
            isPlaying.setValue(true);
        }
    }

    public void playNextMusic() {
        if (musicService == null) return;
        List<MusicBean> list = nowPlayingMusicList.getValue();
        Integer pos = nowPlayingPosition.getValue();
        if (list == null || list.isEmpty() || pos == null) return;

        int nextPos;
        switch (nowPlayingMode.getValue()) {
            case XUNHUAN: nextPos = pos; break;
            case SUIJI: nextPos = (int)(Math.random() * list.size()); break;
            default:
                nextPos = pos + 1 >= list.size() ? 0 : pos + 1;
        }
        MusicBean next = list.get(nextPos);
        musicService.updateCurrentSong(next, nextPos);
        playMusic(next.getId(), nextPos, list);
    }

    public void playPreMusic() {
        if (musicService == null) return;
        List<MusicBean> list = nowPlayingMusicList.getValue();
        Integer pos = nowPlayingPosition.getValue();
        if (list == null || list.isEmpty() || pos == null) return;

        int prePos;
        switch (nowPlayingMode.getValue()) {
            case XUNHUAN: prePos = pos; break;
            case SUIJI: prePos = (int)(Math.random() * list.size()); break;
            default:
                prePos = pos - 1 < 0 ? list.size() - 1 : pos - 1;
        }
        MusicBean pre = list.get(prePos);
        musicService.updateCurrentSong(pre, prePos);
        playMusic(pre.getId(), prePos, list);
    }

    public void seekTo(int progressInSeconds) {
        if (musicService != null) {
            musicService.seekTo(progressInSeconds * 1000);
            nowMusicPlayingProgress.setValue(progressInSeconds);
        }
    }

    public void togglePlayStatus(PlayingMode mode) {
        nowPlayingMode.setValue(mode);
        PreferencesManager.getInstance(getApplication()).putString(KEY_PLAYING_MODE, mode.name());
    }

    // ==============================================================
    //  音质降级重试
    // ==============================================================
    private String lastTriedBitrate = "2000";

    private void tryLowerQuality() {
        String[] qualities = {"2000", "320", "128", "48"};
        int idx = 0;
        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i].equals(lastTriedBitrate)) { idx = i + 1; break; }
        }
        if (idx >= qualities.length) return;
        lastTriedBitrate = qualities[idx];

        MusicBean bean = nowPlayingMusicData.getValue();
        Integer pos = nowPlayingPosition.getValue();
        List<MusicBean> list = nowPlayingMusicList.getValue();
        if (bean == null || pos == null) return;

        Disposable subscribe = new MusicModel()
                .getMusicPlayInfo(bean.getId(), lastTriedBitrate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            String url = MySubString.subString(data, "url=", "sig");
                            musicService.play(url, bean, list, pos);
                            hasPlayedOnce = true;
                            isPlaying.postValue(true);
                            initMusicLyric(bean.getId());
                            savePlayState();
                        },
                        error -> tryLowerQuality());
        disposable.add(subscribe);
    }

    // ==============================================================
    //  歌词加载（使用 Repository）
    // ==============================================================
    private void initMusicLyric(String musicId) {
        if (lyricDisposable != null && !lyricDisposable.isDisposed()) {
            lyricDisposable.dispose();
        }

        // 1. 先从缓存读取
        lyricDisposable = lyricCacheRepository.getLyric(musicId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lyric -> {
                            Log.d(TAG, "从缓存加载歌词");
                            parseAndPostLyric(lyric);
                        },
                        error -> {
                            Log.d(TAG, "缓存未找到，从网络加载歌词: " + musicId);
                            loadLyricFromNetwork(musicId);
                        });
        disposable.add(lyricDisposable);
    }

    private void loadLyricFromNetwork(String musicId) {
        if (lyricDisposable != null && !lyricDisposable.isDisposed()) {
            lyricDisposable.dispose();
        }
        lyricDisposable = new MusicModel().getMusicLyric(musicId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            Log.d(TAG, "网络歌词获取成功，长度: " + (data != null ? data.length() : 0));
                            // 保存到缓存 —— 在 IO 线程执行，带错误处理
                            lyricCacheRepository.saveLyric(musicId, data)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(() -> Log.d(TAG, "歌词缓存已保存"),
                                            throwable -> Log.e(TAG, "保存歌词缓存失败: " + throwable.getMessage()));
                            parseAndPostLyric(data);
                        },
                        error -> {
                            Log.e(TAG, "加载歌词失败: " + error.getMessage());
                            lyricList.postValue(new ArrayList<>());
                            isLyricLoading.postValue(false);
                        });
        disposable.add(lyricDisposable);
    }

    private void parseAndPostLyric(String data) {
        try {
            if (data == null || data.isEmpty()) {
                Log.w(TAG, "歌词数据为空");
                lyricList.postValue(new ArrayList<>());
                isLyricLoading.postValue(false);
                return;
            }
            JSONObject json = new JSONObject(data);
            JSONObject dataObj = json.optJSONObject("data");
            if (dataObj == null) {
                Log.w(TAG, "歌词JSON没有 data 字段: " + data.substring(0, Math.min(100, data.length())));
                lyricList.postValue(new ArrayList<>());
                isLyricLoading.postValue(false);
                return;
            }
            JSONArray lrclist = dataObj.optJSONArray("lrclist");
            if (lrclist == null || lrclist.length() == 0) {
                Log.w(TAG, "歌词 lrclist 为空或不存在");
                lyricList.postValue(new ArrayList<>());
                isLyricLoading.postValue(false);
                return;
            }
            List<LyricEntry> lyrics = new CopyOnWriteArrayList<>();
            for (int i = 0; i < lrclist.length(); i++) {
                JSONObject item = lrclist.getJSONObject(i);
                int time = item.optInt("time", 0);
                String lineLyric = item.optString("lineLyric", "");
                if (!lineLyric.isEmpty()) {
                    lyrics.add(new LyricEntry(time, lineLyric));
                }
            }
            Log.d(TAG, "解析到 " + lyrics.size() + " 行歌词");
            lyricList.postValue(lyrics);
            isLyricLoading.postValue(false);
        } catch (Exception e) {
            Log.e(TAG, "解析歌词失败: " + e.getMessage(), e);
            lyricList.postValue(new ArrayList<>());
            isLyricLoading.postValue(false);
        }
    }

    // ==============================================================
    //  音质信息 / 下载
    // ==============================================================
    public void getMusicDetail() {
        if (nowPlayingMusicData.getValue() == null || isGettingMusicDetail) return;
        isGettingMusicDetail = true;

        Disposable subscribe = new MusicModel().getMusicDetail(nowPlayingMusicData.getValue().getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            try {
                                JSONObject song = new JSONObject(data).getJSONArray("songs").getJSONObject(0);
                                String[] split = song.getString("MINFO").split(";");
                                List<Map<String, String>> tonal = new ArrayList<>();
                                for (String s : split) {
                                    Map<String, String> m = new HashMap<>();
                                    if (s.contains("bitrate:2000")) {
                                        m.put("2000", MySubString.subStartString(s, "size:"));
                                        tonal.add(m);
                                    }
                                    if (s.contains("bitrate:320")) {
                                        m.put("320", MySubString.subStartString(s, "size:"));
                                        tonal.add(m);
                                    }
                                    if (s.contains("bitrate:128")) {
                                        m.put("128", MySubString.subStartString(s, "size:"));
                                        tonal.add(m);
                                    }
                                    if (s.contains("bitrate:48")) {
                                        m.put("48", MySubString.subStartString(s, "size:"));
                                        tonal.add(m);
                                    }
                                }
                                tonal.sort((m1, m2) -> {
                                    String k1 = m1.keySet().iterator().next();
                                    String k2 = m2.keySet().iterator().next();
                                    return Integer.compare(Integer.parseInt(k2), Integer.parseInt(k1));
                                });
                                musicTonalList.postValue(tonal);
                        } catch (Exception e) {
                            Log.e(TAG, "获取音质信息失败", e);
                            musicTonalList.postValue(new ArrayList<>());
                        } finally {
                            isGettingMusicDetail = false;
                        }
                    },
                    error -> {
                        isGettingMusicDetail = false;
                        musicTonalList.postValue(new ArrayList<>());
                    });
        disposable.add(subscribe);
    }

    public void getMusicDownloadUrl(String bitrate) {
        MusicBean cur = nowPlayingMusicData.getValue();
        if (cur == null) return;
        Disposable subscribe = new MusicModel().getMusicDownloadUrl(cur.getId(), bitrate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                            String url = MySubString.subString(data, "url=", "sig");
                            if (url != null) musicDownloadUrl.postValue(url);
                        },
                        error -> {});
        disposable.add(subscribe);
    }

    // ==============================================================
    //  网络检测
    // ==============================================================
    private boolean isOnCellularNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getApplication().getSystemService(Application.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        }
        android.net.NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    // ==============================================================
    //  收藏（使用 Repository）
    // ==============================================================
    private void checkIfLiked(String musicId) {
        favoriteRepository.isFavorite(musicId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(exists -> isLiked.postValue(exists), error -> isLiked.postValue(false));
    }

    public void toggleLike() {
        MusicBean bean = nowPlayingMusicData.getValue();
        if (bean == null) return;

        favoriteRepository.toggleFavorite(bean)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> checkIfLiked(bean.getId()),
                        throwable -> {
                            Log.e(TAG, "切换收藏失败: " + throwable.getMessage());
                            checkIfLiked(bean.getId()); // 刷新状态
                        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (lyricDisposable != null && !lyricDisposable.isDisposed()) lyricDisposable.dispose();
        disposable.clear();
    }
}
