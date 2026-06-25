package com.nafis.groupchat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionWebSocketRequestDTO {
    private Long messageId;
    private String reactionType;
}
