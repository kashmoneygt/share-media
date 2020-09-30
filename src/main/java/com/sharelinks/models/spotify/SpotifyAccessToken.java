package com.sharelinks.models.spotify;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SpotifyAccessToken implements Serializable {
    public String access_token;
    public String token_type;
    public int expires_in;
    public String scope;
    public String refresh_token;
    public LocalDateTime creation_time;
}
