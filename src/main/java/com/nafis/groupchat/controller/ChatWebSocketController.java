package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.ChatMessageDTO;
import com.nafis.groupchat.dto.ReactionEventDTO;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Controller
public class ChatWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Simple Msg Templete
    @MessageMapping("/room/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            ChatMessageDTO message
    ) {

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                message
        );
    }

    @MessageMapping("/room/{roomId}/react")
    public void sendReaction(
            @DestinationVariable Long roomId,
            ReactionEventDTO reactionEvent
    ) {

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/reactions",
                reactionEvent
        );
    }
}