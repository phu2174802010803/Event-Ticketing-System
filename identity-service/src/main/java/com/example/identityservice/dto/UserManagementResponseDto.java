package com.example.identityservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserManagementResponseDto {
    private Integer userId;
    private String username;
    private String email;
    private String role;
    private boolean isActive;
    private String fullName;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}