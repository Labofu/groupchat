package com.nafis.groupchat.service;
import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.ChatRoomRepository;
import com.nafis.groupchat.repository.RoomMemberRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RoomMemberService {
    private final RoomMemberRepository roomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public RoomMemberService(
            RoomMemberRepository roomMemberRepository,
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository
    ) {
        this.roomMemberRepository = roomMemberRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
    }

    public void addMember(Long roomId, Long userId) {

        ChatRoom room =
                chatRoomRepository.findById(roomId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Room not found"
                                )
                        );
        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User not found"
                                )
                        );
        System.out.println("CHECKING DUPLICATE");
        if (roomMemberRepository.existsByRoomAndUser(room, user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User already in room"
            );
        }

        RoomMember roomMember = new RoomMember();

        roomMember.setRoom(room);
        roomMember.setUser(user);

        roomMemberRepository.save(roomMember);
    }

    public List<RoomMember> getMembers(Long roomId) {
        return roomMemberRepository.findByRoom_Id(roomId);
    }
}
