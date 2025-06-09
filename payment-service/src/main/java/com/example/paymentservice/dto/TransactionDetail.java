package com.example.paymentservice.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransactionDetail {
    private String transactionId;
    private Integer userId;
    private String userName;
    private String userEmail;
    private Integer eventId;
    private String eventName;
    private String organizerName;
    private String organizerEmail;
    private Double totalAmount;
    private String paymentMethod;
    private String status;
    private LocalDateTime transactionDate;
    private CustomerInfo customerInfo;
    private EventInfo eventInfo;
    private List<TicketDetail> tickets;

    @Data
    public static class CustomerInfo {
        private Integer userId;
        private String fullName;
        private String email;
        private String phone;
    }

    @Data
    public static class EventInfo {
        private Integer eventId;
        private String eventName;
        private String eventDate;
        private String eventTime;
        private String location;
        private String status;
        private String organizerName;
        private String organizerEmail;
    }

    @Data
    public static class TicketDetail {
        private Integer ticketId;
        private String ticketCode;
        private String status;
        private Double price;
        private LocalDateTime purchaseDate;
        private String areaName;
        private String eventName;
        private LocalDateTime phaseStartTime;
        private LocalDateTime phaseEndTime;
    }
}