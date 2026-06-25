package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.ReactionRequestDTO;
import com.nafis.groupchat.entity.Message;
import com.nafis.groupchat.entity.Reaction;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.MessageRepository;
import com.nafis.groupchat.repository.ReactionRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/rooms/{roomId}/messages/{messageId}/react")
public class ReactionController {

    private final ReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ReactionController(
            ReactionRepository reactionRepository,
            MessageRepository messageRepository,
            UserRepository userRepository
    ) {
        this.reactionRepository = reactionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public String react(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody ReactionRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this room");
        }

        String type = request.getReactionType();

        Optional<Reaction> existing = reactionRepository
                .findByMessageAndUserAndReactionType(message, user, type);

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return "REMOVED";
        } else {
            Reaction reaction = Reaction.builder()
                    .message(message)
                    .user(user)
                    .reactionType(type)
                    .build();
            reactionRepository.save(reaction);
            return "ADDED";
        }
    }
}
