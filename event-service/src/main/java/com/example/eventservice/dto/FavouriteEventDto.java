package com.example.eventservice.dto;

import lombok.Data;

@Data
    public class FavouriteEventDto {
    private Integer eventId;
    private String name;
    private String description;
    private String date;
    private String time;
    private String location;
    private String status;
    private String imageUrl;
    private String bannerUrl;
}