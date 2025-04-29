package com.example.ticketservice.controller;

import com.example.ticketservice.dto.TicketSelectionRequest;
import com.example.ticketservice.dto.TicketSelectionResponse;
import com.example.ticketservice.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String token = authorizationHeader.substring(7); // Loại bỏ "Bearer "
        TicketSelectionResponse response = ticketService.selectTickets(request, userId, token);
        return ResponseEntity.ok(response);
    }
}