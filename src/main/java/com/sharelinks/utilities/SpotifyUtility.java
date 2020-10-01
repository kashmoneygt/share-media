package com.sharelinks.utilities;

import com.google.gson.Gson;
import com.sharelinks.ShareLinksConfig;
import com.sharelinks.models.LinkItem;
import com.sharelinks.models.LinkItemType;
import com.sharelinks.models.spotify.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.util.LinkBrowser;
import okhttp3.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Singleton
public class SpotifyUtility {
    private static final String SPOTIFY_CLIENT_ID = "818391e132f94352828d0de03d7dcdfd";
    private static final String SPOTIFY_AUTHORIZATION_URI = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_REDIRECT_URI = "https://raw.githubusercontent.com/kashmoneygt/share-media/master/SPOTIFY_REDIRECT.md";
    private static final int SPOTIFY_REDIRECT_WAIT_SECONDS = 60;
    private static final String SPOTIFY_TOKEN_API = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_GET_TRACK_API = "https://api.spotify.com/v1/tracks/";
    private static final String TOKEN_FILE = ".spotify_token";
    private static final int TOKEN_EXPIRATION_ADDITIONAL_SECONDS = 60;

    private SpotifyAccessToken accessToken;

    @Inject
    private Client client;

    @Inject
    private ShareLinksConfig config;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ClipboardUtility clipboardUtility;

    @Inject
    private CacheUtility cacheUtility;

    public LinkItem CreateLinkItemFromSpotifyTrackId(String trackId) {
        SpotifyTrack track = GetSpotifyTrack(trackId);
        ImageIcon icon = GetSpotifyIcon(track);
        String artist = track.artists.size() > 0 ? track.artists.get(0).name : "";
        String url = config.spotifyLinkType() == SpotifyLinkType.WEB ? track.external_urls.spotify : track.uri;
        return new LinkItem(LinkItemType.SPOTIFY_LINK, icon, track.name, artist, url, LocalDateTime.now());
    }

    private SpotifyTrack GetSpotifyTrack(String trackId) {
        SetSpotifyAccessToken();
        try {
            Request request = new Request.Builder()
                    .url(SPOTIFY_GET_TRACK_API + trackId)
                    .header("Authorization", accessToken.token_type + " " + accessToken.access_token)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.warn("[External Plugin][Share Links] Error getting track using Spotify access token.");
                return null;
            }

            return new Gson().fromJson(response.body().string(), SpotifyTrack.class);
        } catch (Exception e) {
            log.warn(null, e);
            return null;
        }
    }

    private ImageIcon GetSpotifyIcon(SpotifyTrack track) {
        try {
            SpotifyImage image = track.album.images.stream().filter(i -> i.height == 64).findFirst().orElse(null);
            Request request = new Request.Builder()
                    .url(image.url)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.warn("[External Plugin][Share Links] Error getting image for Spotify track.");
                return null;
            }

            BufferedImage icon = ImageIO.read(response.body().byteStream());
            return new ImageIcon(icon);
        } catch (Exception e) {
            log.warn(null, e);
            return null;
        }
    }

    private void SetSpotifyAccessToken() {
        SpotifyAccessToken accessToken = (SpotifyAccessToken) cacheUtility.ReadObjectFromDisk(TOKEN_FILE);
        if (accessToken != null && IsExpired(accessToken)) {
            accessToken = GetSpotifyAccessTokenFromRefreshToken(accessToken);
        }
        if (accessToken == null) {
            accessToken = GetSpotifyAccessTokenFromAuthorizationFlow();
            cacheUtility.WriteObjectToDisk(accessToken, TOKEN_FILE);
        }

        this.accessToken = accessToken;
    }

    private SpotifyAccessToken GetSpotifyAccessTokenFromAuthorizationFlow() {
        try {
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallange(codeVerifier);
            String state = UUID.randomUUID().toString();

            HttpUrl userAuthUrl = HttpUrl.parse(SPOTIFY_AUTHORIZATION_URI).newBuilder()
                    .addQueryParameter("client_id", SPOTIFY_CLIENT_ID)
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("redirect_uri", SPOTIFY_REDIRECT_URI)
                    .addQueryParameter("code_challenge_method", "S256")
                    .addQueryParameter("code_challenge", codeChallenge)
                    .addQueryParameter("state", state)
                    .build();

            String redirectUriString = PerformUserAuthAndGetRedirectUri(userAuthUrl.toString());
            SpotifyRedirectUri redirectUri = ParseRedirectUri(redirectUriString);
            if (redirectUri == null) {
                log.warn("[External Plugin][Share Links] Error getting a valid redirect URL from user.");
                return null;
            }
            if (!redirectUri.error.isEmpty()) {
                log.warn("[External Plugin][Share Links] RedirectURI returned error=" + redirectUri.error);
                return null;
            }
            if (!redirectUri.state.equalsIgnoreCase(state)) {
                log.warn("[External Plugin][Share Links] RedirectURI state=" + redirectUri.state + " does not equal original state=" + state);
                return null;
            }

            return GetAccessTokenFromUserAuth(redirectUri, codeVerifier);
        } catch (Exception e) {
            log.warn(null, e);
            return null;
        }
    }

    private String PerformUserAuthAndGetRedirectUri(String authorizationUri) {
        try {
            LinkBrowser.browse(authorizationUri);
            return clipboardUtility.GetStringStartingWithSubstringFromClipboard(SPOTIFY_REDIRECT_URI, SPOTIFY_REDIRECT_WAIT_SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "";
    }

    private SpotifyRedirectUri ParseRedirectUri(String redirectUriString) throws URISyntaxException {
        if (redirectUriString.isEmpty()) {
            return null;
        }

        URI redirectUri = new URI(redirectUriString);
        String[] params = redirectUri.getQuery().split("&");
        if (params.length == 2) {
            String[] code = params[0].split("=");
            String[] state = params[1].split("=");
            if (code[0].contains("error")) {
                return new SpotifyRedirectUri("", code[code.length - 1], state[state.length - 1]);
            } else {
                return new SpotifyRedirectUri(code[code.length - 1], "", state[state.length - 1]);
            }
        }

        return null;
    }

    private SpotifyAccessToken GetAccessTokenFromUserAuth(SpotifyRedirectUri redirectUri, String codeVerifier) {
        try {
            RequestBody body = new FormBody.Builder()
                    .add("client_id", SPOTIFY_CLIENT_ID)
                    .add("grant_type", "authorization_code")
                    .add("code", redirectUri.code)
                    .add("redirect_uri", SPOTIFY_REDIRECT_URI)
                    .add("code_verifier", codeVerifier)
                    .build();

            Request request = new Request.Builder()
                    .url(SPOTIFY_TOKEN_API)
                    .post(body)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.warn("[External Plugin][Share Links] Error getting access token from user auth.");
                return null;
            }

            accessToken = new Gson().fromJson(response.body().string(), SpotifyAccessToken.class);
            accessToken.creation_time = LocalDateTime.now();
            return accessToken;
        } catch (Exception e) {
            log.warn(null, e);
            return null;
        }
    }

    private boolean IsExpired(SpotifyAccessToken accessToken) {
        return accessToken.creation_time
                .plusSeconds(accessToken.expires_in)
                .plusSeconds(TOKEN_EXPIRATION_ADDITIONAL_SECONDS)
                .isBefore(LocalDateTime.now());
    }

    private SpotifyAccessToken GetSpotifyAccessTokenFromRefreshToken(SpotifyAccessToken accessToken) {
        // A refresh token that has been obtained through PKCE can be exchanged for an access token only once, after which it becomes invalid.
        try {
            RequestBody body = new FormBody.Builder()
                    .add("client_id", SPOTIFY_CLIENT_ID)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", accessToken.refresh_token)
                    .build();

            Request request = new Request.Builder()
                    .url(SPOTIFY_TOKEN_API)
                    .post(body)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.warn("[External Plugin][Share Links] Error getting access token from refresh token.");
                return null;
            }

            accessToken = new Gson().fromJson(response.body().string(), SpotifyAccessToken.class);
            accessToken.creation_time = LocalDateTime.now();
            return accessToken;
        } catch (Exception e) {
            log.warn(null, e);
            return null;
        }
    }


    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallange(String codeVerifier) throws NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
