package com.nafis.groupchat.repository;

import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository
        extends JpaRepository<Message, Long> {
    List<Message> findByRoomOrderBySentAtAsc(ChatRoom room);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}