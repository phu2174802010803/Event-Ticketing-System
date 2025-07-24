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
@RequestMapping("/api/admin")
public class AdminTicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping("/tickets")
    public ResponseEntity<ResponseWrapper<Page<TicketResponse>>> getTickets(
            @RequestParam(required = false) Integer eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        Page<TicketResponse> tickets = ticketService.getAdminTicketsPaginated(eventId, status, page, size, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Danh sách tất cả vé", tickets));
    }

    @GetMapping("/tickets/by-transaction")
    public ResponseEntity<List<TicketDetail>> getTicketsByTransactionId(
            @RequestParam String transactionId,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        List<TicketDetail> tickets = ticketService.getTicketsByTransactionId(transactionId, token);
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/tickets/scan")
    public ResponseEntity<ResponseWrapper<TicketScanResponse>> scanTicket(
            @RequestBody TicketScanRequest request) {
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "");
        if (!"ADMIN".equals(role)) {
            throw new IllegalStateException("Chỉ Admin có quyền quét vé");
        }
        TicketScanResponse response = ticketService.scanTicket(request, role);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Quét vé thành công", response));
    }

    @GetMapping("/tickets/sales")
    public ResponseEntity<ResponseWrapper<SystemSalesResponseDto>> getSystemSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        SystemSalesResponseDto sales = ticketService.getSystemSales(page, size, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Thống kê bán vé toàn hệ thống", sales));
    }

    @GetMapping("/tickets/events/{eventId}/ticket-stats")
    public ResponseEntity<ResponseWrapper<EventSalesResponseDto>> getEventTicketStats(
            @PathVariable Integer eventId,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        EventSalesResponseDto stats = ticketService.getEventSalesForAdmin(eventId, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Thống kê bán vé chi tiết cho sự kiện", stats));
    }

    @GetMapping("/tickets/events/{eventId}/phase-stats")
    public ResponseEntity<ResponseWrapper<List<PhaseSalesDto>>> getPhaseStats(
            @PathVariable Integer eventId,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        EventSalesResponseDto sales = ticketService.getEventSalesForAdmin(eventId, token);
        return ResponseEntity
                .ok(new ResponseWrapper<>("success", "Thống kê bán vé theo phiên cho sự kiện", sales.getPhases()));
    }

    @GetMapping("/tickets/stats")
    public ResponseEntity<ResponseWrapper<TicketStatsDto>> getTicketStats(
            @RequestHeader("Authorization") String authorizationHeader) {
        TicketStatsDto stats = ticketService.getAdminTicketStats();
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Ticket statistics retrieved successfully", stats));
    }
}