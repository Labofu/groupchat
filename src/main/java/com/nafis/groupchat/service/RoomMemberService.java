package com.nafis.groupchat.service;

import com.nafis.groupchat.dto.RoomMemberDTO;
import com.nafis.groupchat.dto.RoomEventDTO;
import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.ChatRoomRepository;
import com.nafis.groupchat.repository.RoomMemberRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RoomMemberService {
    private final RoomMemberRepository roomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomMemberService(
            RoomMemberRepository roomMemberRepository,
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.roomMemberRepository = roomMemberRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
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

        // Broadcast event: User joined the room
        RoomEventDTO event = RoomEventDTO.builder()
                .message(user.getName() + " joined the room")
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", event);
    }

    public List<RoomMember> getMembers(Long roomId) {
        return roomMemberRepository.findByRoom_Id(roomId);
    }

    public List<RoomMemberDTO> getRoomMembers(Long roomId, String email) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!roomMemberRepository.existsByRoomAndUser(room, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You are not a member of this room.");
        }

        List<RoomMember> memberships = roomMemberRepository.findByRoom_Id(roomId);
        return memberships.stream()
                .map(m -> RoomMemberDTO.builder()
                        .id(m.getUser().getId())
                        .name(m.getUser().getName())
                        .email(m.getUser().getEmail())
                        .build())
                .toList();
    }

    @Transactional
    public void removeMember(Long roomId, Long targetUserId, String email) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Only room creator can remove members
        if (!room.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room creator can remove members");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        // Creator cannot remove themselves
        if (targetUserId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creator cannot remove themselves");
        }

        RoomMember membership = roomMemberRepository.findByRoom_IdAndUser_Id(roomId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member does not belong to the room"));

        roomMemberRepository.delete(membership);

        // Broadcast event: User was removed from the room
        RoomEventDTO event = RoomEventDTO.builder()
                .message(targetUser.getName() + " was removed from the room")
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", event);
    }
}
