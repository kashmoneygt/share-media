package com.sharelinks.models.spotify;

import java.util.List;

public class SpotifyTrack {
    public String name;
    public SpotifyExternalUrl external_urls;
    public SpotifyAlbum album;
    public List<SpotifyArtist> artists;
    public String uri;
}
