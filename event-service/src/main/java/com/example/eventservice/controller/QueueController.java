package com.example.eventservice.controller;

import com.example.eventservice.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private EventService eventService;

    // Tham gia hàng đợi
    @PostMapping("/join/{eventId}")
    public ResponseEntity<String> joinQueue(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String message = eventService.joinQueue(eventId, userId);
        return ResponseEntity.ok(message);
    }

    // Kiểm tra trạng thái hàng đợi
    @GetMapping("/status/{eventId}")
    public ResponseEntity<String> checkQueueStatus(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String status = eventService.checkQueueStatus(eventId, userId);
        return ResponseEntity.ok(status);
    }

    // Xác nhận tiếp tục trong hàng đợi
    @PostMapping("/confirm/{eventId}")
    public ResponseEntity<String> confirmPresence(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        eventService.confirmPresence(eventId, userId);
        return ResponseEntity.ok("Xác nhận thành công");
    }

    // Rời hàng đợi
    @PostMapping("/leave/{eventId}")
    public ResponseEntity<String> leaveQueue(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        eventService.leaveQueue(eventId, userId);
        return ResponseEntity.ok("Bạn đã rời hàng đợi");
    }
}