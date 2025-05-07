package com.example.ticketservice.controller;

import com.example.ticketservice.dto.TicketPurchaseRequest;
import com.example.ticketservice.dto.TicketPurchaseResponse;
import com.example.ticketservice.dto.TicketSelectionRequest;
import com.example.ticketservice.dto.TicketSelectionResponse;
import com.example.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
        String token = authorizationHeader.substring(7); // Loại bỏ "Bearer "
        TicketPurchaseResponse response = ticketService.purchaseTickets(request, userId, token);
        return ResponseEntity.ok(response);
    }
}