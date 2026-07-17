package com.linfeng.music.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 歌词缓存实体（Room）
 */
@Entity(tableName = "lyric_cache")
public class LyricCacheEntity {

    @PrimaryKey
    @NonNull
    public String musicId;

    @NonNull
    public String lyricData;

    public long timestamp;

    public LyricCacheEntity(@NonNull String musicId, @NonNull String lyricData) {
        this.musicId = musicId;
        this.lyricData = lyricData;
        this.timestamp = System.currentTimeMillis();
    }
}
