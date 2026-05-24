package com.doomscroll.wik.dto.response.auth;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private Boolean emailVerified;
}
