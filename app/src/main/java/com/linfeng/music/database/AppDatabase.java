package com.linfeng.music.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.linfeng.music.database.dao.FavoriteDao;
import com.linfeng.music.database.dao.LyricCacheDao;
import com.linfeng.music.database.entity.FavoriteEntity;
import com.linfeng.music.database.entity.LyricCacheEntity;

/**
 * 应用 Room 数据库
 */
@Database(
        entities = {FavoriteEntity.class, LyricCacheEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract FavoriteDao favoriteDao();
    public abstract LyricCacheDao lyricCacheDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "app_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
