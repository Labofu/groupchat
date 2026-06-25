package com.nafis.groupchat.repository;

import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.nafis.groupchat.entity.User;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository
        extends JpaRepository<RoomMember, Long> {

    boolean existsByRoomAndUser(
            ChatRoom room,
            User user
    );

    List<RoomMember> findByRoom_Id(Long roomId);

    List<RoomMember> findByUser(User user);

    List<RoomMember> findByRoom_IdOrderByIdAsc(Long roomId);

    Optional<RoomMember> findByRoom_IdAndUser_Id(Long roomId, Long userId);

    @Modifying
    @Query("DELETE FROM RoomMember rm WHERE rm.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);

    long countByUser(User user);

    int countByRoom_Id(Long roomId);
}