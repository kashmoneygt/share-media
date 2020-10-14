package com.sharelinks;

import com.google.inject.Provides;
import com.sharelinks.models.LinkItem;
import com.sharelinks.utilities.ClipboardUtility;
import com.sharelinks.utilities.DiskUtility;
import com.sharelinks.utilities.SpotifyUtility;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
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
    private static final String SHARE_STRING = "!Share"; // UpperCase "S" -- first letter of all chat messages always seems be UpperCase

    private static final Pattern SPOTIFY_TRACK_PATTERN = Pattern.compile(
            "^(https://open.spotify.com/track/|spotify:track:)(\\w*)\\?*.*", Pattern.CASE_INSENSITIVE);

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private ChatCommandManager chatCommandManager;

    @Inject
    private ShareLinksConfig config;

    @Inject
    private SpotifyUtility spotifyUtility;

    @Inject
    private ClipboardUtility clipboardUtility;

    @Inject
    private DiskUtility diskUtility;

    private ShareLinksPanel shareLinksPanel;

    @Override
    protected void startUp() {
        shareLinksPanel = injector.getInstance(ShareLinksPanel.class);
        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "chain-link.png");
        NavigationButton navButton = NavigationButton.builder()
                .tooltip("Share Links")
                .icon(icon)
                .priority(10)
                .panel(shareLinksPanel)
                .build();
        clientToolbar.addNavigation(navButton);

        diskUtility.CreateShareLinksDir();

        // can't registerCommandAsync() because updateChatMessageWithClipboardLink() needs to finish before onChatMessage() is called
        chatCommandManager.registerCommand(SHARE_STRING, this::updateChatMessageWithClipboardLink);
    }

    @Override
    protected void shutDown() {
        // TODO: save shared links on local disk/RuneLite account
        chatCommandManager.unregisterCommand(SHARE_STRING);
    }

    @Provides
    ShareLinksConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ShareLinksConfig.class);
    }

    /**
     * Listens for player input equaling "!Share" and updates their chatMessage from "!Share" to "!Share <link>"
     * where link is obtained from clipboard.
     *
     * @param chatMessage
     * @param message
     */
    private void updateChatMessageWithClipboardLink(ChatMessage chatMessage, String message) {
        // Whitespace on either sides of "!Share" or lowercase "!share" are OK
        if (message.trim().toLowerCase().equals(SHARE_STRING.toLowerCase())) {
            String clipboardString = clipboardUtility.GetStringFromClipboard();

            Matcher matcher = SPOTIFY_TRACK_PATTERN.matcher(clipboardString);
            if (config.enableSpotifyLinks() && matcher.find()) {
                // "!Share" -> "!Share <link>"
                String messageWithLink = new ChatMessageBuilder()
                        .append(message.trim())
                        .append(" ")
                        .append(matcher.group(0))
                        .build();

                // Update the text of the chatMessage (onChatMessage() picks up new message but old message is rendered)
                // This is the bare minimum to ensure that other players get a chatMessage with the link instead of just "!Share"
                chatMessage.setMessage(messageWithLink);

                // Update the UI component of the chatMessage to maintain consistency (so that players see new message containing link)
                final MessageNode messageNode = chatMessage.getMessageNode();
                messageNode.setRuneLiteFormatMessage(messageWithLink);
                chatMessageManager.update(messageNode);
                client.refreshChat();
            }
        }
    }

    /**
     * Listens for a message of type "!Share <link>" and adds it as an item to the user's Share Links Panel.
     * Has a very low priority of -1 to ensure that message has updated from "!Share" to "!Share <link>".
     *
     * @param chatMessage
     */
    @Subscribe(priority = -1)
    public void onChatMessage(ChatMessage chatMessage) {
        switch (chatMessage.getType()) {
            case AUTOTYPER:
                break;
            case MODCHAT:
            case MODPRIVATECHAT:
            case PUBLICCHAT:
            case PRIVATECHAT:
            case PRIVATECHATOUT:
            case FRIENDSCHAT:
                parseChatMessageAndUpdateLinksPanel(chatMessage);
                break;
            default:
                return;
        }
    }

    private void parseChatMessageAndUpdateLinksPanel(ChatMessage chatMessage) {
        String message = chatMessage.getMessage().trim();
        if (message.toLowerCase().startsWith(SHARE_STRING.toLowerCase())) {
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
