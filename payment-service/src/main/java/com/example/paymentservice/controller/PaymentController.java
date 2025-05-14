package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.util.VNPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request, HttpServletRequest httpRequest) {
        String ipAddr = getClientIp(httpRequest);
        PaymentResponse response = paymentService.processPayment(request, ipAddr);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/return")
    public ResponseEntity<String> handleReturn(HttpServletRequest request) {
        try {
            Map<String, String> params = getParamsFromRequest(request);
            String vnpSecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");

            String calculatedHash = VNPayUtil.getChecksum(params, vnpHashSecret);
            if (!calculatedHash.equals(vnpSecureHash)) {
                return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ");
            }

            String responseCode = params.get("vnp_ResponseCode");
            paymentService.handlePaymentReturn(params);

            if ("00".equals(responseCode)) {
                return ResponseEntity.ok("Thanh toán thành công");
            } else {
                return ResponseEntity.ok("Thanh toán thất bại: Mã lỗi " + responseCode);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý thanh toán: " + e.getMessage());
        }
    }

    private Map<String, String> getParamsFromRequest(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }
        return params;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}