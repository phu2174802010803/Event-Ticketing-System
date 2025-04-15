// src/main/java/com/example/eventservice/dto/EventUpdateRequestDto.java
package com.example.eventservice.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventUpdateRequestDto {
    private String name;
    private String description;
    private LocalDate date;
    private LocalTime time;
    private String location;
    private String imageUrl;
    private String status;
}