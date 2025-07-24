package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.ResponseWrapper;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ResponseWrapper<Notification>> createNotification(@RequestBody NotificationRequest request) {
        Notification notification = notificationService.createNotification(request);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Notification created successfully", notification));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ResponseWrapper<List<Notification>>> getNotificationsByUserId(@PathVariable Integer userId) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Notifications retrieved successfully", notifications));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ResponseWrapper<Notification>> markAsRead(@PathVariable Integer notificationId) {
        Notification notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Notification marked as read", notification));
    }
}