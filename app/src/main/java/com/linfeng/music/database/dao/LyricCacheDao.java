package com.linfeng.music.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.linfeng.music.database.entity.LyricCacheEntity;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * 歌词缓存 DAO
 */
@Dao
public interface LyricCacheDao {

    @Query("SELECT lyricData FROM lyric_cache WHERE musicId = :musicId")
    Single<String> getLyric(String musicId);

    @Query("SELECT EXISTS(SELECT 1 FROM lyric_cache WHERE musicId = :musicId)")
    Single<Boolean> hasLyric(String musicId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(LyricCacheEntity entity);

    @Query("DELETE FROM lyric_cache WHERE musicId = :musicId")
    Completable delete(String musicId);

    @Query("DELETE FROM lyric_cache")
    Completable deleteAll();

    @Query("SELECT COUNT(*) FROM lyric_cache")
    Single<Integer> getCount();
}
