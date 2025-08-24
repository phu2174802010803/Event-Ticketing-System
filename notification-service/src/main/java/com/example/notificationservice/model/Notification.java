package com.example.notificationservice.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "sent_date", nullable = false)
    private LocalDateTime sentDate = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    private String status = "sent";
}