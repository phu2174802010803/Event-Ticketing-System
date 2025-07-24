package com.example.paymentservice.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventPublicDetailDto {
    private Integer eventId;
    private String name;
    private String description;
    private String location;
    private LocalDate date;
    private LocalTime time;
    private String status;
    private String imageUrl;
    private String bannerUrl;
    private Integer organizerId;
    private String organizerName;
    private String organizerEmail;
    private UserPublicDto organizer;
}