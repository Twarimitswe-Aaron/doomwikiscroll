package com.doomscroll.wik.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String bio;
    private String role;
    private String status;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
}
