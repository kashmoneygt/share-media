package com.sharelinks;

import com.sharelinks.models.spotify.SpotifyLinkType;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("sharelinks")
public interface ShareLinksConfig extends Config {
    @ConfigItem(
            position = 0,
            keyName = "enableSpotifyLinks",
            name = "Enable Spotify Links",
            description = "Configures whether sharing Spotify links is enabled"
    )
    default boolean enableSpotifyLinks() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "spotifyLinkType",
            name = "Spotify Links Type",
            description = "Configures whether to open Spotify links in Web or Desktop"
    )
    default SpotifyLinkType spotifyLinkType() {
        return SpotifyLinkType.WEB;
    }
}
