package com.nafis.groupchat.repository;

import com.nafis.groupchat.entity.Message;
import com.nafis.groupchat.entity.Reaction;
import com.nafis.groupchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    
    Optional<Reaction> findByMessageAndUserAndReactionType(
            Message message, 
            User user, 
            String reactionType
    );
    
    List<Reaction> findByMessage(Message message);
}
