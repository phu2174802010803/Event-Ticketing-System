package com.example.identityservice.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String code;
    private String redirectUri;
}