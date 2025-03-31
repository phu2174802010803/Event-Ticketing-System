package com.example.eventservice.dto;

import lombok.Data;

@Data
public class EventResponseDto {
    private Integer eventId;
    private String message;

    public EventResponseDto(Integer eventId, String message) {
        this.eventId = eventId;
        this.message = message;
    }
}
