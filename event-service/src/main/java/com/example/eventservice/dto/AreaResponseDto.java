package com.example.eventservice.dto;

import lombok.Data;

@Data
public class AreaResponseDto {
    private Integer areaId;
    private Integer eventId;
    private Integer templateAreaId;
    private String name;
    private Integer totalTickets;
    private Integer availableTickets;
    private Double price;
}