package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.MessageResponseDTO;
import com.nafis.groupchat.dto.ReactionEventDTO;
import com.nafis.groupchat.dto.ReactionWebSocketRequestDTO;
import com.nafis.groupchat.dto.SendMessageRequestDTO;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.UserRepository;
import com.nafis.groupchat.service.MessageService;
import com.nafis.groupchat.service.ReactionService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ReactionService reactionService;
    private final UserRepository userRepository;

    public ChatWebSocketController(
            SimpMessagingTemplate messagingTemplate,
            MessageService messageService,
            ReactionService reactionService,
            UserRepository userRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.reactionService = reactionService;
        this.userRepository = userRepository;
    }

    @MessageMapping("/room/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload SendMessageRequestDTO message,
            Principal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized WebSocket connection");
        }

        String email = principal.getName();
        
        // MessageService checks if room/user exists, checks room membership, saves, and returns DTO
        MessageResponseDTO savedMessageDto = messageService.sendMessage(roomId, email, message);

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                savedMessageDto
        );
    }

    @MessageMapping("/room/{roomId}/react")
    public void sendReaction(
            @DestinationVariable Long roomId,
            @Payload ReactionWebSocketRequestDTO request,
            Principal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized WebSocket connection");
        }

        String email = principal.getName();

        // ReactionService validates room/user, toggles reaction state in DB, and returns toggled action
        String action = reactionService.toggleReaction(
                roomId, 
                request.getMessageId(), 
                request.getReactionType(), 
                email
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ReactionEventDTO reactionEvent = ReactionEventDTO.builder()
                .messageId(request.getMessageId())
                .senderId(user.getId())
                .senderName(user.getName())
                .reactionType(request.getReactionType())
                .action(action)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/reactions",
                reactionEvent
        );
    }
}