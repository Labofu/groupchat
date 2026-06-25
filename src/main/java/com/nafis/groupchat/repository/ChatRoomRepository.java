package com.nafis.groupchat.repository;

import com.nafis.groupchat.entity.ChatRoom;
import com.nafis.groupchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository
        extends JpaRepository<ChatRoom, Long> {
    long countByCreator(User creator);
}