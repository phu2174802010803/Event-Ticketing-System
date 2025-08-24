package com.example.identityservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendPasswordResetEmail(String to, String token) throws MessagingException {
        Context context = new Context();
        context.setVariable("token", token);
        String htmlContent = templateEngine.process("email/passwordResetEmail", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Đặt lại mật khẩu");
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendDeactivationEmail(String to) throws MessagingException {
        Context context = new Context();
        String htmlContent = templateEngine.process("email/deactivationEmail", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Tài khoản của bạn đã bị vô hiệu hóa");
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}