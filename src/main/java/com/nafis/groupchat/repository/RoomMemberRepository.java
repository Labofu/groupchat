package com.nafis.groupchat.repository;

import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nafis.groupchat.entity.User;

import java.util.List;

public interface RoomMemberRepository
        extends JpaRepository<RoomMember, Long> {

    boolean existsByRoomAndUser(
            ChatRoom room,
            User user
    );

    List<RoomMember> findByRoom_Id(Long roomId);

}