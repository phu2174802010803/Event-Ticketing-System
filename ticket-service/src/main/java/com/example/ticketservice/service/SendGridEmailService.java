package com.example.ticketservice.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.util.List;

@Service
public class SendGridEmailService {

    @Value("${sendgrid.api.key}")
    private String apiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Event Tickets}")
    private String fromName;

    private final SpringTemplateEngine templateEngine;

    public SendGridEmailService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void sendTicketConfirmationEmail(String to, String userName, String eventName,
            String eventLocation, String eventDateTime,
            List<TicketEmailInfo> tickets,
            String transactionId, Double totalAmount) throws IOException {

        System.out.println("=== SENDING TICKET CONFIRMATION EMAIL VIA SENDGRID ===");
        System.out.println("To: " + to);
        System.out.println("From: " + fromEmail);
        System.out.println("API Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "null"));

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

        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        String subject = "Xác nhận vé sự kiện: " + eventName;
        Content content = new Content("text/html", htmlContent);

        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            System.out.println("SendGrid Response Status: " + response.getStatusCode());
            System.out.println("SendGrid Response Body: " + response.getBody());
            System.out.println("=== EMAIL SENT SUCCESSFULLY ===");

            if (response.getStatusCode() >= 400) {
                throw new IOException("SendGrid API Error: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (IOException ex) {
            System.err.println("Error sending email: " + ex.getMessage());
            throw ex;
        }
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