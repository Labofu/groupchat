package com.nafis.groupchat.service;

import com.nafis.groupchat.dto.CreateRoomRequest;
import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.ChatRoomRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public ChatRoomService(
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
    }

    public ChatRoom createRoom(
            CreateRoomRequest request,
            String email
    ) {
        System.out.println("Room Name = " + request.getRoomName());

        User creator = userRepository
                .findByEmail(email)
                .orElseThrow();

        ChatRoom room =
                new ChatRoom(
                        request.getRoomName(),
                        creator
                );

        return chatRoomRepository.save(room);
    }

    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }
}