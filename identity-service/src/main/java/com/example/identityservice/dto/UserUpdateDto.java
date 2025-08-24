package com.example.identityservice.dto;

import lombok.Data;

@Data
public class UserUpdateDto {
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String role;
    private Boolean isActive;
}