package com.sharelinks;

import com.sharelinks.models.LinkItem;
import com.sharelinks.utilities.CacheUtility;
import com.sharelinks.utilities.ClipboardUtility;
import com.sharelinks.utilities.MessageUtility;
import com.sharelinks.utilities.SpotifyUtility;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@PluginDescriptor(
        name = "Share Links"
)
public class ShareLinksPlugin extends Plugin {
    private static final String SHARE_STRING = "!Share";

    private static final Pattern SPOTIFY_TRACK_PATTERN = Pattern.compile(
            "^(https://open.spotify.com/track/|spotify:track:)(\\w*)\\?*.*", Pattern.CASE_INSENSITIVE);


    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ShareLinksConfig config;

    @Inject
    private Client client;

    @Inject
    private SpotifyUtility spotifyUtility;

    @Inject
    private ClipboardUtility clipboardUtility;

    @Inject
    private MessageUtility messageUtility;

    @Inject
    private CacheUtility cacheUtility;

    private ShareLinksPanel shareLinksPanel;

    @Override
    protected void startUp() {
        shareLinksPanel = injector.getInstance(ShareLinksPanel.class);
        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "link-resized.png");
        NavigationButton navButton = NavigationButton.builder()
                .tooltip("Share Links")
                .icon(icon)
                .priority(10)
                .panel(shareLinksPanel)
                .build();
        clientToolbar.addNavigation(navButton);

        cacheUtility.CreateShareLinksDir();
    }

    @Override
    protected void shutDown() {
        // For saving shared links on local disk/RuneLite account
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getType() == ChatMessageType.AUTOTYPER
                || (chatMessage.getType() != ChatMessageType.MODCHAT
                && chatMessage.getType() != ChatMessageType.PUBLICCHAT
                && chatMessage.getType() != ChatMessageType.PRIVATECHAT
                && chatMessage.getType() != ChatMessageType.PRIVATECHATOUT
                && chatMessage.getType() != ChatMessageType.MODPRIVATECHAT
                && chatMessage.getType() != ChatMessageType.FRIENDSCHAT)) {
            return;
        }

        String message = chatMessage.getMessage().trim();
        if (message.startsWith(SHARE_STRING)) {
            // If the local user typed "!share", check their clipboard
            // for a valid link and update their message to contain the link
            if (message.length() == SHARE_STRING.length()
                    && client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
                    && chatMessage.getName().replace(" ", " ").contains(client.getLocalPlayer().getName())) {
                String clipboardString = clipboardUtility.GetStringFromClipboard();

                Matcher matcher = SPOTIFY_TRACK_PATTERN.matcher(clipboardString);
                if (config.enableSpotifyLinks() && matcher.find()) {
                    // Update message from "!share" to "!share <link>" so that the current user's panel also gets updated
                    // Updating the chat message seems to be more performant than deleting this message and sending a new one
                    message += " " + matcher.group(0);
                    messageUtility.UpdateChatMessage(chatMessage, message);
                }
            }

            // If someone other than current user typed "!share <link>" OR current user typed "!share"
            // (message now contains link), add the link to the user's panel
            if (message.length() > SHARE_STRING.length()) {
                String link = message.substring(SHARE_STRING.length()).trim();

                Matcher matcher = SPOTIFY_TRACK_PATTERN.matcher(link);
                if (config.enableSpotifyLinks() && matcher.find()) {
                    SwingUtilities.invokeLater(() ->
                    {
                        LinkItem spotifyItem = spotifyUtility.CreateLinkItemFromSpotifyTrackId(matcher.group(2));
                        shareLinksPanel.addItemToPanel(spotifyItem);
                    });
                }
            }
        }
    }

}
