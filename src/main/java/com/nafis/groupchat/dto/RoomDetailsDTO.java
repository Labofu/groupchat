package com.nafis.groupchat.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDetailsDTO {
    private Long id;
    private String roomName;
    private LocalDateTime createdAt;
    private RoomMemberDTO creator;
    private int memberCount;
    private List<RoomMemberDTO> members;
}
