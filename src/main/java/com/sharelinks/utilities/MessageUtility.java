package com.sharelinks.utilities;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;

@Slf4j
@Singleton
public class MessageUtility {

    @Inject
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;

    public String GetStringFromClipboard() {
        String clipboardString;
        try {
            clipboardString = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getContents(null)
                    .getTransferData(DataFlavor.stringFlavor).toString();
        } catch (Exception e) {
            log.warn("[External Plugin][Share Links] Error reading user's clipboard.");
            clipboardString = "";
        }

        return clipboardString;
    }

    public void UpdateChatMessage(ChatMessage chatMessage, String updatedString) {
        String response = new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append(updatedString)
                .build();

        final MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setRuneLiteFormatMessage(response);
        chatMessageManager.update(messageNode);
        client.refreshChat();
    }
}
