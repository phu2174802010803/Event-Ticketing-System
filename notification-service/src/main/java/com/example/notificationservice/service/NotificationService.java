package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.PaymentConfirmationEvent;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setMessage(request.getMessage());
        notification.setStatus("sent");
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByUserId(Integer userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Notification markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setStatus("read");
        return notificationRepository.save(notification);
    }

    @KafkaListener(topics = "payment-confirmations", groupId = "notification-group")
    public void handlePaymentConfirmation(PaymentConfirmationEvent event) {
        String message = event.getStatus().equals("completed")
                ? "Thanh toán thành công cho giao dịch: " + event.getTransactionId()
                : "Thanh toán thất bại cho giao dịch: " + event.getTransactionId();
        NotificationRequest request = new NotificationRequest();
        request.setUserId(event.getUserId());
        request.setMessage(message);
        createNotification(request);
    }
}