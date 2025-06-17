package com.example.ticketservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendTicketConfirmationEmail(String to, String userName, String eventName,
                                            String eventLocation, String eventDateTime,
                                            List<TicketEmailInfo> tickets,
                                            String transactionId, Double totalAmount) throws MessagingException {

        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("eventName", eventName);
        context.setVariable("eventLocation", eventLocation);
        context.setVariable("eventDateTime", eventDateTime);
        context.setVariable("tickets", tickets);
        context.setVariable("transactionId", transactionId);
        context.setVariable("totalAmount", totalAmount);

        System.out.println("=== PROCESSING TEMPLATE ===");
        String htmlContent = templateEngine.process("email/ticketConfirmationEmail", context);
        System.out.println("HTML Content length: " + htmlContent.length());

        System.out.println("=== SENDING EMAIL ===");
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Xác nhận vé sự kiện: " + eventName);
        helper.setText(htmlContent, true);
        mailSender.send(message);
        System.out.println("=== EMAIL SENT SUCCESSFULLY ===");
    }

    @Data
    public static class TicketEmailInfo {
        private String ticketCode;
        private String areaName;
        private Double price;
        private String purchaseDate;
        private String qrCodeDataURL;

        public TicketEmailInfo(String ticketCode, String areaName, Double price, String purchaseDate) {
            this.ticketCode = ticketCode;
            this.areaName = areaName;
            this.price = price;
            this.purchaseDate = purchaseDate;
        }

        // Constructor với QR code
        public TicketEmailInfo(String ticketCode, String areaName, Double price, String purchaseDate,
                               String qrCodeDataURL) {
            this.ticketCode = ticketCode;
            this.areaName = areaName;
            this.price = price;
            this.purchaseDate = purchaseDate;
            this.qrCodeDataURL = qrCodeDataURL;
        }
    }
}