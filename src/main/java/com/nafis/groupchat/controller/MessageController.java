package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.MessageResponseDTO;
import com.nafis.groupchat.dto.SendMessageRequestDTO;
import com.nafis.groupchat.entity.Message;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.nafis.groupchat.service.MessageService;

import java.util.List;


@RestController
@RequestMapping("/rooms")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }
    @PostMapping("/{roomId}/messages")
    public MessageResponseDTO sendMessage(
            @PathVariable Long roomId,
            @RequestBody SendMessageRequestDTO request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return messageService.sendMessage(
                roomId,
                email,
                request
        );
    }

    @GetMapping("/{roomId}/messages")
    public List<MessageResponseDTO> getMessages(
            @PathVariable Long roomId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return messageService.getMessages(
                roomId,
                email
        );
    }
}
