package com.linfeng.music.bean;

public class MusicBean {
    private String id, name, artist, albumIcon;
    private String playUrl;
    private boolean like;

    public MusicBean() {
    }

    public MusicBean(String id, String name, String artist, String albumIcon, boolean like) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.albumIcon = albumIcon;
        this.like = like;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumIcon() {
        return albumIcon;
    }

    public void setAlbumIcon(String albumIcon) {
        this.albumIcon = albumIcon;
    }

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public boolean isLike() {
        return like;
    }

    public void setLike(boolean like) {
        this.like = like;
    }
}
