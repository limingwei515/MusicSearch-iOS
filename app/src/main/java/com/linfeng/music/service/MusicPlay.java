package com.linfeng.music.service;

public interface MusicPlay {
    /**
     * 进度更新
     *
     * @param currentPosition 当前播放进度（毫秒）
     * @param duration        总时长（毫秒）
     */
    void onProgressUpdate(int currentPosition, int duration);
}
