package com.sharelinks.utilities;

import ca.weblite.webview.WebViewCLIClient;
import com.google.gson.Gson;
import com.sharelinks.ShareLinksConfig;
import com.sharelinks.models.LinkItem;
import com.sharelinks.models.LinkItemType;
import com.sharelinks.models.spotify.SpotifyAccessToken;
import com.sharelinks.models.spotify.SpotifyImage;
import com.sharelinks.models.spotify.SpotifyLinkType;
import com.sharelinks.models.spotify.SpotifyTrack;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import okhttp3.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class SpotifyUtility {
    private static final String SPOTIFY_CLIENT_ID = "818391e132f94352828d0de03d7dcdfd";
    private static final String SPOTIFY_AUTHORIZATION_URI = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_REDIRECT_URI = "https://www.spotify.com";
    private static final String SPOTIFY_TOKEN_API = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_GET_TRACK_API = "https://api.spotify.com/v1/tracks/";

    private SpotifyAccessToken accessToken;

    @Inject
    private Client client;

    @Inject
    private ShareLinksConfig config;

    @Inject
    private OkHttpClient okHttpClient;

    public LinkItem CreateLinkItemFromSpotifyTrackId(String trackId) {
        SpotifyTrack track = GetSpotifyTrack(trackId);
        ImageIcon icon = GetSpotifyIcon(track);
        String artist = track.artists.size() > 0 ? track.artists.get(0).name : "";
        String url = config.spotifyLinkType() == SpotifyLinkType.WEB ? track.external_urls.spotify : track.uri;
        return new LinkItem(LinkItemType.SPOTIFY_LINK, icon, track.name, artist, url, LocalDateTime.now());
    }

    private SpotifyTrack GetSpotifyTrack(String trackId) {
        // TODO : get token
        GetSpotifyAccessToken();
        try {
            Request getTrackRequest = new Request.Builder()
                    .url(SPOTIFY_GET_TRACK_API + trackId)
                    .header("Authorization", accessToken.token_type + " " + accessToken.access_token)
                    .build();

            Response getTrackResponse = okHttpClient.newCall(getTrackRequest).execute();
            if (!getTrackResponse.isSuccessful()) {
                log.warn("[External Plugin][Share Links] Error getting track using Spotify access token.");
                return null;
            }

            return new Gson().fromJson(getTrackResponse.body().string(), SpotifyTrack.class);
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

    public void GetSpotifyAccessToken() {
        try {
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallange(codeVerifier);
            String state = UUID.randomUUID().toString();

            HttpUrl url = HttpUrl.parse(SPOTIFY_AUTHORIZATION_URI).newBuilder()
                    .addQueryParameter("client_id", SPOTIFY_CLIENT_ID)
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("redirect_uri", SPOTIFY_REDIRECT_URI)
                    .addQueryParameter("code_challenge_method", "S256")
                    .addQueryParameter("code_challenge", codeChallenge)
                    .addQueryParameter("state", state)
                    .build();

            // Because we are creating a WebView pop-up, we should do it on the Event Dispatching Thread
            SwingUtilities.invokeLater(() -> {
                try {
                    String redirectUri = PerformUserAuthAndGetRedirectUri(url.toString()).get();
                    log.warn("Hi: " + redirectUri);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });


        } catch (Exception e) {
            log.warn(null, e);
        }
    }

    private CompletableFuture<String> PerformUserAuthAndGetRedirectUri(String authorizationUri) {
        // TODO : open browser the normal way and point to a github README saying to copy link and close browser
        AtomicReference<String> redirectUri = new AtomicReference<>("");
        WebViewCLIClient webview = (WebViewCLIClient) new WebViewCLIClient.Builder()
                .url(authorizationUri)
                .title("Spotify User Authorization")
                .size(client.getCanvasWidth() / 2, client.getCanvasHeight() / 2)
                .build();

        webview.addLoadListener(evt -> {
            log.warn(evt.getURL());
            if (evt.getURL().startsWith(SPOTIFY_REDIRECT_URI)) {
                log.warn("got redirect uri");
                redirectUri.set(evt.getURL());

            }
        });
        // it waits 60 seconds...
//            TimeUnit.SECONDS.sleep(60);
        while (redirectUri.get().isEmpty()) {
        }
        log.warn("done...");
        try {
            log.warn("closing...");
            webview.close();
            log.warn("closed...");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(redirectUri.get());
    }

    private CompletableFuture<Void> CloseWebView(WebViewCLIClient webview) {
        try {
            TimeUnit.SECONDS.sleep(5);
            webview.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

    private void temp() {
//        Response response = okHttpClient.newCall(request).execute();
//        if (!response.isSuccessful()) {
//            log.warn("[External Plugin][Share Links] Error getting Spotify access token.");
//        }

//            accessToken = new Gson().fromJson(response.body().string(), SpotifyAccessToken.class);
//            accessToken.creation_time = LocalDateTime.now();
    }

    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallange(String codeVerifier) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
