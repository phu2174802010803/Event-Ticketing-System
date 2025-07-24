package com.example.eventservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class MapDetailDto {
    private Integer mapWidth;
    private Integer mapHeight;
    private List<AreaDetailDto> areas;
}
