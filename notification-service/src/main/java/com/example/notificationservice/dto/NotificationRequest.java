package com.example.notificationservice.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Integer userId;
    private String message;
}