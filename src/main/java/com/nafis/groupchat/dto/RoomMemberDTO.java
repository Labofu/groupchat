package com.nafis.groupchat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMemberDTO {
    private Long id;
    private String name;
    private String email;
}
