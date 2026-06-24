package com.nafis.groupchat.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDTO {

    private Long id;
    private String content;
    private LocalDateTime sentAt;

    private Long senderId;
    private String senderName;
}