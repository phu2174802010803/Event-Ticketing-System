package com.example.identityservice.dto;

import lombok.Data;

@Data
public class UserResponseDto {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;

    public UserResponseDto(Integer userId, String username, String email, String fullName, String phone, String address) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }
}