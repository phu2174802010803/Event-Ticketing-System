package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketResponse {
    private String ticketCode;         // Mã vé (URL mã QR)
    private String status;             // Trạng thái vé
    private String purchaseDate;       // Ngày mua
    private Double price;              // Giá vé
    private String transactionId;      // Mã giao dịch
    private String eventName;          // Tên sự kiện (lấy động từ event-service)
    private String areaName;           // Tên khu vực (lấy động từ event-service)
    private String phaseStartTime;     // Thời gian bắt đầu phiên bán vé
    private String phaseEndTime;       // Thời gian kết thúc phiên bán vé
    private String userFullName;       // Họ tên người dùng (cho Organizer/Admin)
    private String userEmail;          // Email người dùng (cho Organizer/Admin)
    private Integer ticketId;          // ID vé (chỉ cho Admin)
    private Integer eventId;           // ID sự kiện (chỉ cho Admin)
    private Integer areaId;            // ID khu vực (chỉ cho Admin)
    private Integer phaseId;           // ID phiên bán vé (chỉ cho Admin)
    private Integer userId;            // ID người dùng (chỉ cho Admin)
    private String eventDate;          // Ngày sự kiện
    private String eventTime;          // Giờ sự kiện
    private String eventLocation;      // Địa điểm sự kiện
    private String paymentMethod;      // Phương thức thanh toán
}