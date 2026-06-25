package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.AddMemberRequest;
import com.nafis.groupchat.dto.CreateRoomRequest;
import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
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
    public ChatRoom createRoom(
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

    @GetMapping("/{roomId}/members")
    public List<RoomMember> getMembers(
            @PathVariable Long roomId
    ) {
        return roomMemberService.getMembers(roomId);
    }

    @GetMapping
    public List<ChatRoom> getAllRooms(Authentication authentication) {
        if (authentication == null) {
            return java.util.Collections.emptyList();
        }
        return chatRoomService.getRoomsForUser(authentication.getName());
    }
}