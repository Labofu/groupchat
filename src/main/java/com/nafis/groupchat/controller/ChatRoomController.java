package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.AddMemberRequest;
import com.nafis.groupchat.dto.CreateRoomRequest;
import com.nafis.groupchat.dto.RoomMemberDTO;
import com.nafis.groupchat.dto.RoomDetailsDTO;
import com.nafis.groupchat.dto.RoomResponseDTO;
import com.nafis.groupchat.service.ChatRoomService;
import com.nafis.groupchat.service.RoomMemberService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final RoomMemberService roomMemberService;

    public ChatRoomController(
            ChatRoomService chatRoomService,
            RoomMemberService roomMemberService
    ) {
        this.chatRoomService = chatRoomService;
        this.roomMemberService = roomMemberService;
    }

    @PostMapping
    public RoomResponseDTO createRoom(
            @RequestBody CreateRoomRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return chatRoomService.createRoom(
                request,
                email
        );
    }

    @PostMapping("/{roomId}/members")
    public String addMember(
            @PathVariable Long roomId,
            @RequestBody AddMemberRequest request
    ) {
        try {
            roomMemberService.addMember(roomId, request.getUserId());
            return "Member Added";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    @GetMapping("/{roomId}")
    public RoomDetailsDTO getRoomDetails(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return chatRoomService.getRoomDetails(roomId, email);
    }

    @GetMapping("/{roomId}/members")
    public List<RoomMemberDTO> getMembers(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return roomMemberService.getRoomMembers(roomId, email);
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public String removeMember(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        roomMemberService.removeMember(roomId, userId, email);
        return "Member removed successfully";
    }

    @DeleteMapping("/{roomId}/leave")
    public String leaveRoom(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        chatRoomService.leaveRoom(roomId, email);
        return "You left the room successfully";
    }

    @GetMapping
    public List<RoomResponseDTO> getAllRooms(Authentication authentication) {
        if (authentication == null) {
            return java.util.Collections.emptyList();
        }
        return chatRoomService.getRoomsForUser(authentication.getName());
    }
}