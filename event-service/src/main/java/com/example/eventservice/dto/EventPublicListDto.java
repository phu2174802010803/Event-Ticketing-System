package com.example.eventservice.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventPublicListDto {
    private Integer eventId;      // ID sự kiện
    private String name;          // Tên sự kiện
    private LocalDate date;       // Ngày diễn ra
    private LocalTime time;       // Giờ diễn ra
    private String location;      // Địa điểm
    private String imageUrl;      // URL hình ảnh
    private String bannerUrl;     // URL banner
}