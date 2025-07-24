package com.example.identityservice.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;

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

    public void sendPasswordResetEmail(String to, String token) throws IOException {
        System.out.println("=== SENDING PASSWORD RESET EMAIL VIA SENDGRID ===");
        System.out.println("To: " + to);
        System.out.println("From: " + fromEmail);
        System.out.println("API Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "null"));

        Context context = new Context();
        context.setVariable("token", token);
        String htmlContent = templateEngine.process("email/passwordResetEmail", context);

        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        String subject = "Đặt lại mật khẩu";
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

    public void sendDeactivationEmail(String to) throws IOException {
        System.out.println("=== SENDING DEACTIVATION EMAIL VIA SENDGRID ===");
        System.out.println("To: " + to);

        Context context = new Context();
        String htmlContent = templateEngine.process("email/deactivationEmail", context);

        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        String subject = "Tài khoản của bạn đã bị vô hiệu hóa";
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
            System.out.println("=== EMAIL SENT SUCCESSFULLY ===");

            if (response.getStatusCode() >= 400) {
                throw new IOException("SendGrid API Error: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (IOException ex) {
            System.err.println("Error sending email: " + ex.getMessage());
            throw ex;
        }
    }
}