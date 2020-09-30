package com.sharelinks.models.spotify;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SpotifyRedirectUri {
    public String code;
    public String error;
    public String state;
}
