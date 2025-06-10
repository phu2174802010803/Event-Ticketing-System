package com.example.eventservice.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class EventPublicDetailDto {
    private Integer eventId;      // ID sự kiện
    private String name;          // Tên sự kiện
    private String description;   // Mô tả sự kiện
    private LocalDate date;       // Ngày diễn ra
    private LocalTime time;       // Giờ diễn ra
    private String location;      // Địa điểm
    private String imageUrl;      // URL hình ảnh
    private String bannerUrl;      // URL banner
    private CategoryResponseDto category; // Thông tin danh mục
    private OrganizerPublicDto organizer;
    private List<SellingPhaseResponseDto> sellingPhases;
}