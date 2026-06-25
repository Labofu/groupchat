package com.nafis.groupchat.repository;

import com.nafis.groupchat.entity.Message;
import com.nafis.groupchat.entity.Reaction;
import com.nafis.groupchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    
    Optional<Reaction> findByMessageAndUserAndReactionType(
            Message message, 
            User user, 
            String reactionType
    );
    
    List<Reaction> findByMessage(Message message);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.message.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
