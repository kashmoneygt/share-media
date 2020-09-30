package com.sharelinks.models.spotify;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SpotifyLinkType {
    WEB("Web"),
    DESKTOP("Desktop");

    private final String type;

    @Override
    public String toString() {
        return type;
    }
}
