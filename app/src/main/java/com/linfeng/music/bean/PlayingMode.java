package com.linfeng.music.bean;

import com.linfeng.music.R;

public enum PlayingMode {
    SHUNXU(R.drawable.icon_shunxu),      // 列表循环
    SUIJI(R.drawable.icon_suiji),  // 顺序播放
    XUNHUAN(R.drawable.icon_xunhuan);  // 单曲循环

    private final int iconRes;

    PlayingMode(int iconRes) {
        this.iconRes = iconRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public PlayingMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
