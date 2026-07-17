package com.linfeng.music.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.linfeng.music.database.entity.FavoriteEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 收藏音乐 DAO
 */
@Dao
public interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY createTime DESC")
    Flowable<List<FavoriteEntity>> getAllFavorites();

    @Query("SELECT * FROM favorites ORDER BY createTime DESC")
    Single<List<FavoriteEntity>> getAllFavoritesOnce();

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE rid = :rid)")
    Single<Boolean> exists(String rid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(FavoriteEntity entity);

    @Query("DELETE FROM favorites WHERE rid = :rid")
    Completable deleteByRid(String rid);

    @Query("SELECT COUNT(*) FROM favorites")
    Single<Integer> getCount();
}
