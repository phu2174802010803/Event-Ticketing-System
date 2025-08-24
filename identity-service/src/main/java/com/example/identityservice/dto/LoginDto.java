package com.example.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDto {
    @NotBlank(message = "Tên đăng nhập hoặc email là bắt buộc")
    private String login;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;
}