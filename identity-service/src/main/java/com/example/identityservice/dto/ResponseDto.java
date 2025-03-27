package com.example.identityservice.dto;

import lombok.Data;

@Data
public class ResponseDto {
    private Integer userId;
    private String message;

    public ResponseDto(Integer userId, String message) {
        this.userId = userId;
        this.message = message;
    }
}