package com.nafis.groupchat.service;

import com.nafis.groupchat.dto.CreateRoomRequest;
import com.nafis.groupchat.dto.RoomMemberDTO;
import com.nafis.groupchat.dto.RoomDetailsDTO;
import com.nafis.groupchat.dto.RoomEventDTO;
import com.nafis.groupchat.dto.RoomResponseDTO;
import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.ChatRoomRepository;
import com.nafis.groupchat.repository.MessageRepository;
import com.nafis.groupchat.repository.ReactionRepository;
import com.nafis.groupchat.repository.RoomMemberRepository;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatRoomService(
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository,
            RoomMemberRepository roomMemberRepository,
            MessageRepository messageRepository,
            ReactionRepository reactionRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public RoomResponseDTO createRoom(
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

        RoomMemberDTO creatorDTO = RoomMemberDTO.builder()
                .id(creator.getId())
                .name(creator.getName())
                .email(creator.getEmail())
                .build();

        return RoomResponseDTO.builder()
                .id(savedRoom.getId())
                .roomName(savedRoom.getRoomName())
                .createdAt(savedRoom.getCreatedAt())
                .creator(creatorDTO)
                .memberCount(1)
                .build();
    }

    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    public List<RoomResponseDTO> getRoomsForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<RoomMember> memberships = roomMemberRepository.findByUser(user);
        return memberships.stream()
                .map(m -> {
                    ChatRoom room = m.getRoom();
                    int count = roomMemberRepository.countByRoom_Id(room.getId());
                    RoomMemberDTO creatorDTO = RoomMemberDTO.builder()
                            .id(room.getCreator().getId())
                            .name(room.getCreator().getName())
                            .email(room.getCreator().getEmail())
                            .build();

                    return RoomResponseDTO.builder()
                            .id(room.getId())
                            .roomName(room.getRoomName())
                            .createdAt(room.getCreatedAt())
                            .creator(creatorDTO)
                            .memberCount(count)
                            .build();
                })
                .toList();
    }

    public RoomDetailsDTO getRoomDetails(Long roomId, String email) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!roomMemberRepository.existsByRoomAndUser(room, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You are not a member of this room.");
        }

        List<RoomMember> memberships = roomMemberRepository.findByRoom_Id(roomId);
        List<RoomMemberDTO> memberDTOs = memberships.stream()
                .map(m -> RoomMemberDTO.builder()
                        .id(m.getUser().getId())
                        .name(m.getUser().getName())
                        .email(m.getUser().getEmail())
                        .build())
                .toList();

        RoomMemberDTO creatorDTO = RoomMemberDTO.builder()
                .id(room.getCreator().getId())
                .name(room.getCreator().getName())
                .email(room.getCreator().getEmail())
                .build();

        return RoomDetailsDTO.builder()
                .id(room.getId())
                .roomName(room.getRoomName())
                .createdAt(room.getCreatedAt())
                .creator(creatorDTO)
                .memberCount(memberDTOs.size())
                .members(memberDTOs)
                .build();
    }

    @Transactional
    public void leaveRoom(Long roomId, String email) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        RoomMember membership = roomMemberRepository.findByRoom_IdAndUser_Id(roomId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room"));

        roomMemberRepository.delete(membership);

        // Broadcast event: User left the room
        RoomEventDTO leftEvent = RoomEventDTO.builder()
                .message(user.getName() + " left the room")
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", leftEvent);

        // Check if leaving user was the creator
        if (room.getCreator().getId().equals(user.getId())) {
            List<RoomMember> remainingMembers = roomMemberRepository.findByRoom_IdOrderByIdAsc(roomId);

            if (remainingMembers.isEmpty()) {
                // Delete all associated messages, reactions, memberships, and the room itself
                reactionRepository.deleteByRoomId(roomId);
                messageRepository.deleteByRoomId(roomId);
                roomMemberRepository.deleteByRoomId(roomId);
                chatRoomRepository.delete(room);
            } else {
                // Transfer ownership to oldest remaining member
                User newCreator = remainingMembers.get(0).getUser();
                room.setCreator(newCreator);
                chatRoomRepository.save(room);

                // Broadcast event: New creator notice
                RoomEventDTO creatorEvent = RoomEventDTO.builder()
                        .message("Room creator changed to " + newCreator.getName())
                        .build();
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", creatorEvent);
            }
        }
    }
}