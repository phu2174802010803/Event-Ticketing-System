package com.example.ticketservice.dto;

import lombok.Data;


@Data
public class TicketDetail {
    private Integer ticketId;
    private String ticketCode;
    private String status;
    private String purchaseDate;
    private Double price;
    private String eventName;
    private String areaName;
    private String phaseStartTime;     // Thời gian bắt đầu phiên bán vé
    private String phaseEndTime;       // Thời gian kết thúc phiên bán vé

    // Constructor
    public TicketDetail() {
    }
}