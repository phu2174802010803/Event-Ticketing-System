// src/main/java/com/example/eventservice/dto/EventDetailResponseDto.java
package com.example.eventservice.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventDetailResponseDto {
    private Integer eventId;
    private Integer categoryId;
    private Integer organizerId;
    private Integer mapTemplateId;
    private String name;
    private String description;
    private LocalDate date;
    private LocalTime time;
    private String location;
    private String imageUrl;
    private String status;
}