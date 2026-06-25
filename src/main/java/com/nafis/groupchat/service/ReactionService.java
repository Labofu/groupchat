package com.nafis.groupchat.service;

import com.nafis.groupchat.entity.Message;
import com.nafis.groupchat.entity.Reaction;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.MessageRepository;
import com.nafis.groupchat.repository.ReactionRepository;
import com.nafis.groupchat.repository.RoomMemberRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;

    public ReactionService(
            ReactionRepository reactionRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            RoomMemberRepository roomMemberRepository
    ) {
        this.reactionRepository = reactionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    public String toggleReaction(Long roomId, Long messageId, String reactionType, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this room");
        }

        if (!roomMemberRepository.existsByRoomAndUser(message.getRoom(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room");
        }

        Optional<Reaction> existing = reactionRepository
                .findByMessageAndUserAndReactionType(message, user, reactionType);

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return "REMOVED";
        } else {
            Reaction reaction = Reaction.builder()
                    .message(message)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            reactionRepository.save(reaction);
            return "ADDED";
        }
    }
}
