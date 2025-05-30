package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class AreaSalesDto {
    private Integer areaId;
    private String areaName;
    private Integer totalTickets;
    private Integer soldTickets;
    private Integer availableTickets;
    private Double price;
}