package com.linfeng.music.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.linfeng.music.bean.MusicBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyLikeMusicDB extends SQLiteOpenHelper {
    public MyLikeMusicDB(@Nullable Context context) {
        super(context, "my_music_like", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建 musicLike 表
        String CREATE_TABLE_MUSICLIKE = "CREATE TABLE musiclike (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "artist TEXT NOT NULL, " +
                "album_icon TEXT NOT NULL, " +
                "rid TEXT NOT NULL);";
        db.execSQL(CREATE_TABLE_MUSICLIKE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public boolean checkMusicExists(String rid) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 1 FROM musiclike WHERE rid = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{rid})) {
            return cursor.moveToFirst();
        }
    }

    public void insertMusic(Map<String, String> dataMap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            values.put(entry.getKey(), entry.getValue());
        }
        db.insert("musiclike", null, values);
    }

    public void deleteMusic(String rid) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = "rid = ?";
        String[] selectionArgs = {rid};
        db.delete("musiclike", selection, selectionArgs);
    }

    public List<MusicBean> queryMusic() {
        List<MusicBean> musicList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT name, artist, album_icon, rid FROM musiclike ORDER BY id DESC";
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                String albumIcon = cursor.getString(cursor.getColumnIndexOrThrow("album_icon"));
                String rid = cursor.getString(cursor.getColumnIndexOrThrow("rid"));
                MusicBean musicLike = new MusicBean(rid, name, artist, albumIcon, true);
                musicList.add(musicLike);
            }
        }
        return musicList;
    }
}
