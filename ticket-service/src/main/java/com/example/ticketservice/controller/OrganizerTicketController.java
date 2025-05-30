package com.example.ticketservice.controller;

import com.example.ticketservice.dto.*;
import com.example.ticketservice.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerTicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping("/tickets/history")
    public ResponseEntity<ResponseWrapper<List<TicketHistoryResponse>>> getTicketHistory(
            @RequestParam Integer eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        List<TicketHistoryResponse> history = ticketService.getTicketHistoryForOrganizer(organizerId, eventId, status, page, size);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Lịch sử vé của sự kiện", history));
    }

    @PostMapping("/tickets/scan")
    public ResponseEntity<ResponseWrapper<TicketScanResponse>> scanTicket(
            @RequestBody TicketScanRequest request) {
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "");
        if (!"ORGANIZER".equals(role)) {
            throw new IllegalStateException("Chỉ Organizer mới có quyền quét vé");
        }
        TicketScanResponse response = ticketService.scanTicket(request, role);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Quét vé thành công", response));
    }

    @GetMapping("/tickets/events/{eventId}/sales")
    public ResponseEntity<ResponseWrapper<EventSalesResponseDto>> getEventSales(
            @PathVariable Integer eventId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String token = authorizationHeader.substring(7);
        EventSalesResponseDto sales = ticketService.getEventSalesForOrganizer(eventId, organizerId, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Ticket sales statistics for event", sales));
    }
}