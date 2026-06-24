package com.nafis.groupchat.repository;

import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository
        extends JpaRepository<Message, Long> {
    List<Message> findByRoomOrderBySentAtAsc(ChatRoom room);

}