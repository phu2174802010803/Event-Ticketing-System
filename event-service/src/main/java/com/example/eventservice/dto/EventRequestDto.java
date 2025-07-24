package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class EventRequestDto {
    private Integer categoryId;

    @NotBlank(message = "Tên sự kiện là bắt buộc")
    private String name;

    private String description;

    @NotNull(message = "Ngày sự kiện là bắt buộc")
    private LocalDate date;

    @NotNull(message = "Thời gian sự kiện là bắt buộc")
    private LocalTime time;

    @NotBlank(message = "Địa điểm sự kiện là bắt buộc")
    private String location;

    private Integer mapTemplateId;

    private String imageUrl;

    private String bannerUrl;

    private String status; //Chỉ dùng cho admin

    private List<AreaRequestDto> areas; //Danh sách khu vực
}
