package com.linfeng.music.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class LyricCacheDB extends SQLiteOpenHelper {
    public LyricCacheDB(@Nullable Context context) {
        super(context, "lyric_cache", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_LYRIC_CACHE = "CREATE TABLE lyric_cache (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "music_id TEXT NOT NULL UNIQUE, " +
                "lyric_data TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL);";
        db.execSQL(CREATE_TABLE_LYRIC_CACHE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS lyric_cache");
        onCreate(db);
    }

    public void saveLyric(String musicId, String lyricData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("music_id", musicId);
        values.put("lyric_data", lyricData);
        values.put("timestamp", System.currentTimeMillis());
        db.insertWithOnConflict("lyric_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getLyric(String musicId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT lyric_data FROM lyric_cache WHERE music_id = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{musicId})) {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("lyric_data"));
            }
        }
        return null;
    }

    public boolean hasLyricCache(String musicId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 1 FROM lyric_cache WHERE music_id = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{musicId})) {
            return cursor.moveToFirst();
        }
    }

    public void deleteLyric(String musicId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = "music_id = ?";
        String[] selectionArgs = {musicId};
        db.delete("lyric_cache", selection, selectionArgs);
    }

    public void clearAllCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("lyric_cache", null, null);
    }
}
