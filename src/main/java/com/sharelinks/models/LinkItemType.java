package com.sharelinks.models;

public enum LinkItemType {
    SPOTIFY_LINK("Spotify"),
    YOUTUBE_LINK("YouTube");

    public String value;

    LinkItemType(String value) {
        this.value = value;
    }
}
