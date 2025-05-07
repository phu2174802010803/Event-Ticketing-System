package com.example.eventservice.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class SellingPhaseRequestDto {
    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    private LocalDateTime endTime;

    @NotNull(message = "Số lượng vé là bắt buộc")
    private Integer ticketsAvailable;
}