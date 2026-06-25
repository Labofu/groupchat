package com.nafis.groupchat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionEventDTO {
    private Long messageId;
    private Long senderId;
    private String senderName;
    private String reactionType;
    private String action; // "ADD" or "REMOVE"
}
