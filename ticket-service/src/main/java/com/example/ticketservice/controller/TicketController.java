package com.example.ticketservice.controller;

import com.example.ticketservice.dto.*;
import com.example.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/select")
    public ResponseEntity<TicketSelectionResponse> selectTickets(
            @RequestBody TicketSelectionRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        try {
            Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            String token = authorizationHeader.substring(7);
            TicketSelectionResponse response = ticketService.selectTickets(request, userId, token);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            TicketSelectionResponse errorResponse = new TicketSelectionResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            TicketSelectionResponse errorResponse = new TicketSelectionResponse();
            errorResponse.setMessage("Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/purchase")
    public ResponseEntity<TicketPurchaseResponse> purchaseTickets(
            @Valid @RequestBody TicketPurchaseRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String token = authorizationHeader.substring(7);
        TicketPurchaseResponse response = ticketService.purchaseTickets(request, userId, token);
        return ResponseEntity.ok(response); // Trả về 200 OK với body JSON
    }

    @GetMapping("/{ticketId}/qr")
    public ResponseEntity<ResponseWrapper<TicketQRResponse>> getTicketQR(@PathVariable Integer ticketId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        TicketQRResponse qrResponse = ticketService.getTicketQR(ticketId, userId, "USER");
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Mã QR của vé", qrResponse));
    }

    @GetMapping
    public ResponseEntity<ResponseWrapper<List<TicketResponse>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String token = authorizationHeader.substring(7);
        List<TicketResponse> tickets = ticketService.getUserTickets(userId, status, page, size, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Danh sách vé của bạn", tickets));
    }
}