package com.example.ticketservice.controller;

import com.example.ticketservice.dto.*;
import com.example.ticketservice.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerTicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping("/tickets")
    public ResponseEntity<ResponseWrapper<Page<TicketResponse>>> getTickets(
            @RequestParam Integer eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authorizationHeader) {
        Integer organizerId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String token = authorizationHeader.substring(7);
        Page<TicketResponse> tickets = ticketService.getOrganizerTicketsPaginated(organizerId, eventId, status, page,
                size, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Danh sách vé của organizer", tickets));
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
        Integer organizerId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String token = authorizationHeader.substring(7);
        EventSalesResponseDto sales = ticketService.getEventSalesForOrganizer(eventId, organizerId, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Thống kê bán vé cho sự kiện", sales));
    }

    @GetMapping("/tickets/events/{eventId}/phase-stats")
    public ResponseEntity<ResponseWrapper<List<PhaseSalesDto>>> getPhaseStats(
            @PathVariable Integer eventId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Integer organizerId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication()
                        .getPrincipal());
        String token = authorizationHeader.substring(7);
        EventSalesResponseDto sales = ticketService.getEventSalesForOrganizer(eventId, organizerId, token);
        return ResponseEntity
                .ok(new ResponseWrapper<>("success", "Thống kê bán vé theo phiên cho sự kiện",
                        sales.getPhases()));
    }

    @GetMapping("/tickets/stats")
    public ResponseEntity<ResponseWrapper<TicketStatsDto>> getTicketStats(
            @RequestHeader("Authorization") String authorizationHeader) {
        Integer organizerId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication()
                        .getPrincipal());
        String token = authorizationHeader.substring(7);
        TicketStatsDto stats = ticketService.getOrganizerTicketStats(organizerId, token);
        return ResponseEntity.ok(
                new ResponseWrapper<>("success", "Ticket statistics retrieved successfully", stats));
    }
}