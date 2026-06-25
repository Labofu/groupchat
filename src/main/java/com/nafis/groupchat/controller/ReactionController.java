package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.ReactionRequestDTO;
import com.nafis.groupchat.service.ReactionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms/{roomId}/messages/{messageId}/react")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping
    public String react(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody ReactionRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return reactionService.toggleReaction(roomId, messageId, request.getReactionType(), email);
    }
}
