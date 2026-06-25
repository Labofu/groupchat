package com.nafis.groupchat.service;

import com.nafis.groupchat.dto.CreateRoomRequest;
import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.ChatRoomRepository;
import com.nafis.groupchat.repository.RoomMemberRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;

    public ChatRoomService(
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository,
            RoomMemberRepository roomMemberRepository
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
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

        ChatRoom savedRoom = chatRoomRepository.save(room);

        RoomMember creatorMember = new RoomMember();
        creatorMember.setRoom(savedRoom);
        creatorMember.setUser(creator);
        roomMemberRepository.save(creatorMember);

        return savedRoom;
    }

    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    public List<ChatRoom> getRoomsForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<RoomMember> memberships = roomMemberRepository.findByUser(user);
        return memberships.stream().map(RoomMember::getRoom).toList();
    }
}