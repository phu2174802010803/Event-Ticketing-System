package com.example.ticketservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventPublicDetailDto {
    private Integer eventId;
    private String name;
    private String description;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private String imageUrl;
    private Integer organizerId;
    private String organizerName;
}