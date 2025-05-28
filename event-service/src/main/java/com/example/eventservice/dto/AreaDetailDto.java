package com.example.eventservice.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AreaDetailDto {
    private Integer areaId;
    private Integer templateAreaId; // Thêm để khớp với MapTemplate.tsx
    private String name;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Integer totalTickets;
    private Integer availableTickets;
    private Double price;
    private List<Map<String, Float>> vertices; // Định dạng [{x: float, y: float}, ...]
    private String zone;
    private String fillColor;
}