package com.example.identityservice.dto;

import lombok.Value;

@Value
public class AuthenticationResponse {
    String jwt;
    String message;
    String username;
}