package com.nafis.groupchat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {

    private Long id;
    private String content;
    private String senderName;
    private Long senderId;
    private String senderEmail;
}