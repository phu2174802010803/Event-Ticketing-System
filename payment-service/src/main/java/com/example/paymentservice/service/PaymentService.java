package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.model.Transaction;
import com.example.paymentservice.repository.TransactionRepository;
import com.example.paymentservice.util.VNPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.paymentUrl}")
    private String vnpPaymentUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    @Value("${ticket.service.url:http://localhost:8084}")
    private String ticketServiceUrl;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String ipAddr) {
        // Lấy userId từ SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdStr = (String) authentication.getPrincipal();
        Integer userId = Integer.parseInt(userIdStr);

        // Tạo giao dịch
        Transaction transaction = new Transaction();
        transaction.setTransactionId(request.getTransactionId());
        transaction.setUserId(userId); // Lấy từ SecurityContext
        transaction.setEventId(request.getEventId()); // Gán eventId từ PaymentRequest
        transaction.setTotalAmount(request.getAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("pending");
        transactionRepository.save(transaction);

        // Tạo URL thanh toán VNPay
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf((long) (request.getAmount() * 100)));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", request.getTransactionId());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang: " + request.getTransactionId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", ipAddr);
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        String vnpSecureHash = VNPayUtil.getChecksum(vnpParams, vnpHashSecret);
        vnpParams.put("vnp_SecureHash", vnpSecureHash);

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .append("&");
        }
        if (query.length() > 0) {
            query.deleteCharAt(query.length() - 1);
        }

        String paymentUrl = vnpPaymentUrl + "?" + query.toString();
        return new PaymentResponse(paymentUrl, "pending", "Yêu cầu thanh toán đã được tạo");
    }

    @Transactional
    public void handlePaymentReturn(Map<String, String> params) {
        String transactionId = params.get("vnp_TxnRef");

        // Lấy thông tin giao dịch từ database
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Giao dịch không tồn tại"));

        // Cập nhật trạng thái giao dịch
        String responseCode = params.get("vnp_ResponseCode");
        String status = "00".equals(responseCode) ? "completed" : "failed";
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        // Gửi yêu cầu xác nhận đến ticket service
        try {
            Map<String, Object> confirmation = new HashMap<>();
            confirmation.put("transactionId", transactionId);
            confirmation.put("status", status);
            confirmation.put("userId", transaction.getUserId()); // Thêm userId để xác thực ở ticket service
            confirmation.put("eventId", transaction.getEventId()); // Thêm eventId

            // Gửi thông tin đến ticket-service để cập nhật trạng thái vé
            restTemplate.postForObject(ticketServiceUrl + "/api/tickets/confirm", confirmation, String.class);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không làm gián đoạn quy trình callback
            System.err.println("Lỗi khi gửi xác nhận đến ticket service: " + e.getMessage());
        }
    }
}