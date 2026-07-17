package com.linfeng.music.bean;

public class SongBean {
    private String songName;
    private String songImgUrl;
    private String songId;
    /**
     * 歌单的总播放次数
     */
    private String listencnt;

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongImgUrl() {
        return songImgUrl;
    }

    public void setSongImgUrl(String songImgUrl) {
        if (songImgUrl == null || songImgUrl.length() < 5) {
            this.songImgUrl = "https://img1.kuwo.cn/star/albumcover/default.jpg";
        } else if (!songImgUrl.startsWith("http")) {
            this.songImgUrl = "https://img4.kuwo.cn/star/albumcover/" + songImgUrl;
        } else {
            this.songImgUrl = songImgUrl;
        }
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getListencnt() {
        return listencnt;
    }

    public void setListencnt(String listencnt) {
        this.listencnt = listencnt;
    }
}
