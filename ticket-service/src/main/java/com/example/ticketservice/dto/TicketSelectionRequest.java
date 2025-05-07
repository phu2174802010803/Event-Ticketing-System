package com.example.ticketservice.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class TicketSelectionRequest {
    @NotNull(message = "Event ID là bắt buộc")
    private Integer eventId;

    @NotNull(message = "Area ID là bắt buộc")
    private Integer areaId;

    @NotNull(message = "Phase ID là bắt buộc")
    private Integer phaseId;

    @NotNull(message = "Số lượng vé là bắt buộc")
    @Min(value = 1, message = "Số lượng vé phải lớn hơn 0")
    @Max(value = 4, message = "Số lượng vé tối đa là 4")
    private Integer quantity;
}