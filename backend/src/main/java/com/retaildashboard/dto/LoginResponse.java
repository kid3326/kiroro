package com.retaildashboard.dto;

import com.retaildashboard.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 로그인 응답 DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UUID userId;
    private String username;
    private String email;
    private Role role;
    private String assignedBrand;
    private LocalDateTime expiresAt;
}
