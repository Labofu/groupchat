package com.nafis.groupchat.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponseDTO {
    private Long id;
    private String roomName;
    private LocalDateTime createdAt;
    private RoomMemberDTO creator;
    private int memberCount;
}
