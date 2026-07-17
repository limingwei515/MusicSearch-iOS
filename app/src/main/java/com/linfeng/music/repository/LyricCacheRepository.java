package com.linfeng.music.repository;

import android.content.Context;

import com.linfeng.music.database.AppDatabase;
import com.linfeng.music.database.dao.LyricCacheDao;
import com.linfeng.music.database.entity.LyricCacheEntity;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 歌词缓存数据仓库
 * 封装 Room 歌词缓存表的访问
 */
public class LyricCacheRepository {

    private static volatile LyricCacheRepository INSTANCE;
    private final LyricCacheDao lyricCacheDao;

    private LyricCacheRepository(Context context) {
        lyricCacheDao = AppDatabase.getInstance(context).lyricCacheDao();
    }

    public static LyricCacheRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LyricCacheRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LyricCacheRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 获取缓存的歌词
     */
    public Single<String> getLyric(String musicId) {
        return lyricCacheDao.getLyric(musicId)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 检查歌词是否已缓存
     */
    public Single<Boolean> hasLyric(String musicId) {
        return lyricCacheDao.hasLyric(musicId)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 保存歌词到缓存
     */
    public Completable saveLyric(String musicId, String lyricData) {
        return lyricCacheDao.insert(new LyricCacheEntity(musicId, lyricData))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 删除指定歌词缓存
     */
    public Completable deleteLyric(String musicId) {
        return lyricCacheDao.delete(musicId)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 清空所有歌词缓存
     */
    public Completable clearAll() {
        return lyricCacheDao.deleteAll()
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取缓存的歌词数量
     */
    public Single<Integer> getCount() {
        return lyricCacheDao.getCount()
                .subscribeOn(Schedulers.io());
    }
}
