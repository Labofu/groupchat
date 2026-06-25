package com.nafis.groupchat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionDTO {
    private Long userId;
    private String userName;
    private String reactionType;
}
