package com.nafis.groupchat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String name;
    private String email;
    private long joinedRooms;
    private long createdRooms;
}
