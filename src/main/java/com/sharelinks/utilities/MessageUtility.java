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

@Slf4j
@Singleton
public class MessageUtility {

    @Inject
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;

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
