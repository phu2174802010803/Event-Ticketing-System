package com.example.ticketservice.controller;

import com.example.ticketservice.dto.ResponseWrapper;
import com.example.ticketservice.dto.TicketHistoryResponse;
import com.example.ticketservice.dto.TicketScanRequest;
import com.example.ticketservice.dto.TicketScanResponse;
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
}