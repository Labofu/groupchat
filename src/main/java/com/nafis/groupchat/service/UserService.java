package com.nafis.groupchat.service;

import com.nafis.groupchat.dto.UserProfileDTO;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.ChatRoomRepository;
import com.nafis.groupchat.repository.RoomMemberRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;

    public UserService(
            UserRepository userRepository,
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository
    ) {
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        long createdRooms = chatRoomRepository.countByCreator(user);
        long joinedRooms = roomMemberRepository.countByUser(user);

        return UserProfileDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdRooms(createdRooms)
                .joinedRooms(joinedRooms)
                .build();
    }
}
