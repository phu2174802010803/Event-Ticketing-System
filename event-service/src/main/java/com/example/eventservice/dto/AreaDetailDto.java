package com.example.eventservice.dto;

import lombok.Data;

@Data
public class AreaDetailDto {
    private Integer areaId;
    private String name;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Integer totalTickets;
    private Integer availableTickets;
    private Double price;
}