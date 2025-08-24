package com.example.ticketservice.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class TicketPurchaseRequest {
    @NotBlank(message = "Transaction ID là bắt buộc")
    private String transactionId;
}