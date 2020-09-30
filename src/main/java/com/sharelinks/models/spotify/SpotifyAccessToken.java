package com.sharelinks.models.spotify;

import java.time.LocalDateTime;

public class SpotifyAccessToken {
    public String access_token;
    public String token_type;
    public int expires_in;
    public String scope;
    public LocalDateTime creation_time;
}
