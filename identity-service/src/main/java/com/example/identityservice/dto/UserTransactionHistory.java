package com.example.identityservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserTransactionHistory {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private List<TransactionDetail> transactions;

    @Data
    public static class TransactionDetail {
        private String transactionId;
        private Integer eventId;
        private Double totalAmount;
        private String paymentMethod;
        private String status;
        private String transactionDate;
        private List<TicketDetail> tickets;
    }

    @Data
    public static class TicketDetail {
        private String ticketCode;
        private String status;
        private String purchaseDate;
        private Double price;
        private String eventName;
        private String areaName;
    }
}