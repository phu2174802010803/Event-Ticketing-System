package com.example.paymentservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDto {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String role;
    private LocalDateTime createdAt;
    private Boolean isActive;

    public UserResponseDto(Integer userId, String username, String email, String fullName, String phone,
                           String address, String role, LocalDateTime createdAt, Boolean isActive) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }
}