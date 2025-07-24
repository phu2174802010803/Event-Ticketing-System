package com.example.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String email;
    @NotBlank
    private String role;
    @NotBlank
    private String fullName;
    @NotBlank
    private String phone;
    @NotBlank
    private String address;
}