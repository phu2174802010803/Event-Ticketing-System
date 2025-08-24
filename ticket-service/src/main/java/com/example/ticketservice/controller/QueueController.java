package com.example.ticketservice.controller;

import com.example.ticketservice.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;

    // Tham gia hàng đợi
    @PostMapping("/join/{eventId}")
    public ResponseEntity<String> joinQueue(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String message = queueService.joinQueue(eventId, userId);
        return ResponseEntity.ok(message);
    }

    // Kiểm tra trạng thái hàng đợi
    @GetMapping("/status/{eventId}")
    public ResponseEntity<String> checkQueueStatus(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String status = queueService.checkQueueStatus(eventId, userId);
        return ResponseEntity.ok(status);
    }

    // Xác nhận tiếp tục trong hàng đợi
    @PostMapping("/confirm/{eventId}")
    public ResponseEntity<String> confirmPresence(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        queueService.confirmPresence(eventId, userId);
        return ResponseEntity.ok("Xác nhận thành công");
    }

    // Rời hàng đợi
    @PostMapping("/leave/{eventId}")
    public ResponseEntity<String> leaveQueue(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        queueService.leaveQueue(eventId, userId);
        return ResponseEntity.ok("Bạn đã rời hàng đợi");
    }

    @GetMapping("/availability/{eventId}")
    public ResponseEntity<Boolean> checkQueueAvailability(@PathVariable Integer eventId) {
        boolean canJoin = queueService.canJoinQueue(eventId);
        return ResponseEntity.ok(canJoin);
    }
}