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

    public MessageService(
            MessageRepository messageRepository,
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository,
            RoomMemberRepository roomMemberRepository
    ) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    public void sendMessage(
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

        messageRepository.save(message);

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
                .map(message -> MessageResponseDTO.builder()
                        .id(message.getId())
                        .content(message.getContent())
                        .sentAt(message.getSentAt())
                        .senderId(message.getSender().getId())
                        .senderName(message.getSender().getName())
                        .build())
                .toList();
    }

}