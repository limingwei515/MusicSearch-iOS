package com.linfeng.music.repository;

import android.content.Context;

import com.linfeng.music.bean.MusicBean;
import com.linfeng.music.database.AppDatabase;
import com.linfeng.music.database.dao.FavoriteDao;
import com.linfeng.music.database.entity.FavoriteEntity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 收藏数据仓库
 * 封装 Room 收藏表的访问，提供业务友好的 API
 */
public class FavoriteRepository {

    private static volatile FavoriteRepository INSTANCE;
    private final FavoriteDao favoriteDao;

    private FavoriteRepository(Context context) {
        favoriteDao = AppDatabase.getInstance(context).favoriteDao();
    }

    public static FavoriteRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FavoriteRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FavoriteRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 查询所有收藏歌曲，返回 MusicBean 列表
     */
    public Single<List<MusicBean>> getAllFavoritesAsMusicBeans() {
        return favoriteDao.getAllFavoritesOnce()
                .subscribeOn(Schedulers.io())
                .map(favorites -> {
                    List<MusicBean> beans = new ArrayList<>();
                    for (FavoriteEntity f : favorites) {
                        MusicBean bean = new MusicBean();
                        bean.setId(f.rid);
                        bean.setName(f.name);
                        bean.setArtist(f.artist);
                        bean.setAlbumIcon(f.albumIcon.isEmpty() ? null : f.albumIcon);
                        bean.setLike(true);
                        beans.add(bean);
                    }
                    return beans;
                });
    }

    /**
     * 查询是否已收藏
     */
    public Single<Boolean> isFavorite(String musicId) {
        return favoriteDao.exists(musicId)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 添加收藏
     */
    public Completable addFavorite(MusicBean bean) {
        return favoriteDao.insert(new FavoriteEntity(
                bean.getId(),
                bean.getName(),
                bean.getArtist(),
                bean.getAlbumIcon() == null ? "" : bean.getAlbumIcon()
        )).subscribeOn(Schedulers.io());
    }

    /**
     * 移除收藏
     */
    public Completable removeFavorite(String musicId) {
        return favoriteDao.deleteByRid(musicId)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 切换收藏状态
     */
    public Completable toggleFavorite(MusicBean bean) {
        return favoriteDao.exists(bean.getId())
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(exists -> {
                    if (exists) {
                        return favoriteDao.deleteByRid(bean.getId())
                                .subscribeOn(Schedulers.io());
                    } else {
                        return favoriteDao.insert(new FavoriteEntity(
                                bean.getId(), bean.getName(), bean.getArtist(),
                                bean.getAlbumIcon() == null ? "" : bean.getAlbumIcon()
                        )).subscribeOn(Schedulers.io());
                    }
                });
    }

    /**
     * 获取收藏数量
     */
    public Single<Integer> getCount() {
        return favoriteDao.getCount()
                .subscribeOn(Schedulers.io());
    }
}
