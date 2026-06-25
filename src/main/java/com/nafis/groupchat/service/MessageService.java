package com.nafis.groupchat.service;

import com.nafis.groupchat.dto.MessageResponseDTO;
import com.nafis.groupchat.dto.SendMessageRequestDTO;
import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.Message;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.ChatRoomRepository;
import com.nafis.groupchat.repository.MessageRepository;
import com.nafis.groupchat.repository.RoomMemberRepository;
import com.nafis.groupchat.repository.UserRepository;
import com.nafis.groupchat.repository.ReactionRepository;
import com.nafis.groupchat.dto.ReactionDTO;
import com.nafis.groupchat.entity.Reaction;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ReactionRepository reactionRepository;

    public MessageService(
            MessageRepository messageRepository,
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository,
            RoomMemberRepository roomMemberRepository,
            ReactionRepository reactionRepository
    ) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.reactionRepository = reactionRepository;
    }

    public MessageResponseDTO sendMessage(
            Long roomId,
            String email,
            SendMessageRequestDTO request
    ) {
        ChatRoom room =
                chatRoomRepository.findById(roomId)
                        .orElseThrow();

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();

        if (!roomMemberRepository.existsByRoomAndUser(room, user)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this room"
            );
        }


        Message message = new Message();

        message.setContent(request.getContent());
        message.setSentAt(LocalDateTime.now());
        message.setSender(user);
        message.setRoom(room);

        Message savedMessage = messageRepository.save(message);

        return MessageResponseDTO.builder()
                .id(savedMessage.getId())
                .content(savedMessage.getContent())
                .sentAt(savedMessage.getSentAt())
                .senderId(savedMessage.getSender().getId())
                .senderName(savedMessage.getSender().getName())
                .reactions(java.util.Collections.emptyList())
                .build();

    }

    public List<MessageResponseDTO> getMessages(
            Long roomId,
            String email
    ) {

        ChatRoom room =
                chatRoomRepository.findById(roomId)
                        .orElseThrow();

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();

        if (!roomMemberRepository.existsByRoomAndUser(room, user)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this room"
            );
        }

        List<Message> messages =
                messageRepository.findByRoomOrderBySentAtAsc(room);

        return messages.stream()
                .map(message -> {
                    List<Reaction> reactions = reactionRepository.findByMessage(message);
                    List<ReactionDTO> reactionDTOs = reactions.stream()
                            .map(r -> ReactionDTO.builder()
                                    .userId(r.getUser().getId())
                                    .userName(r.getUser().getName())
                                    .reactionType(r.getReactionType())
                                    .build())
                            .toList();

                    return MessageResponseDTO.builder()
                            .id(message.getId())
                            .content(message.getContent())
                            .sentAt(message.getSentAt())
                            .senderId(message.getSender().getId())
                            .senderName(message.getSender().getName())
                            .senderEmail(message.getSender().getEmail())
                            .reactions(reactionDTOs)
                            .build();
                })
                .toList();
    }

}