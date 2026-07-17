package com.linfeng.music.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 收藏音乐实体（Room）
 */
@Entity(tableName = "favorites")
public class FavoriteEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String rid;

    @NonNull
    public String name;

    @NonNull
    public String artist;

    @NonNull
    public String albumIcon;

    public long createTime;

    public FavoriteEntity(@NonNull String rid, @NonNull String name,
                          @NonNull String artist, @NonNull String albumIcon) {
        this.rid = rid;
        this.name = name;
        this.artist = artist;
        this.albumIcon = albumIcon;
        this.createTime = System.currentTimeMillis();
    }
}
