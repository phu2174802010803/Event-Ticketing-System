package com.example.paymentservice.service;

import com.example.paymentservice.dto.*;
import com.example.paymentservice.model.Transaction;
import com.example.paymentservice.repository.TransactionRepository;
import com.example.paymentservice.util.VNPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EventClient eventClient;

    @Autowired
    private TicketClient ticketClient;

    @Autowired
    private KafkaTemplate<String, PaymentConfirmationEvent> kafkaTemplate;

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
            PaymentConfirmationEvent event = new PaymentConfirmationEvent();
            event.setTransactionId(transactionId);
            event.setStatus(status);
            event.setUserId(transaction.getUserId());
            event.setEventId(transaction.getEventId());
            kafkaTemplate.send("payment-confirmations", "paymentconfirmation", event);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không làm gián đoạn quy trình callback
            System.err.println("Lỗi khi gửi xác nhận đến ticket service: " + e.getMessage());
        }
    }

    public Page<Transaction> getOrganizerTransactions(Integer organizerId, String token, int page, int size) {
        List<Integer> eventIds = eventClient.getOrganizerEventIds(organizerId, token);
        return transactionRepository.findByEventIdIn(eventIds, PageRequest.of(page, size));
    }

    public Page<Transaction> getAllTransactions(int page, int size) {
        return transactionRepository.findAll(PageRequest.of(page, size));
    }

    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    public FinancialReportDto generateFinancialReport(LocalDateTime startDate, LocalDateTime endDate) {
        //Lấy tất cả giao dịch trong khoảng thời gian
        List<Transaction> allTransactions = transactionRepository.findAll();

        //Kiểm tra xem có giao dịch nào trong khoảng thời gian không
        List<Transaction> transactions = allTransactions
                .stream()
                .filter(t -> (t.getTransactionDate().isEqual(startDate) || t.getTransactionDate().isAfter(startDate)) &&
                        (t.getTransactionDate().isEqual(endDate) || t.getTransactionDate().isBefore(endDate)) &&
                        "completed".equals(t.getStatus()))
                .toList();

        if (transactions.isEmpty()) {
            //Nếu không có giao dịch trả về báo cáo trống
            FinancialReportDto emptyReport = new FinancialReportDto();
            emptyReport.setTotalRevenue(0.0);
            emptyReport.setTotalTransactions(0);
            emptyReport.setAverageTransactionAmount(0.0);
            return emptyReport;
        }

        // Tính toán tổng doanh thu và số lượng giao dịch
        Double totalRevenue = transactions.stream()
                .mapToDouble(Transaction::getTotalAmount)
                .sum();

        Long totalTransactions = (long) transactions.size();

        System.out.println("Total revenue: " + totalRevenue + ", Total transactions: " + totalTransactions);

        FinancialReportDto report = new FinancialReportDto();
        report.setTotalRevenue(totalRevenue);
        report.setTotalTransactions(totalTransactions.intValue());
        report.setAverageTransactionAmount(totalTransactions > 0 ? totalRevenue / totalTransactions : 0.0);
        return report;
    }

    public Page<Transaction> getTransactionsByUserId(Integer userId, int page, int size) {
        return transactionRepository.findByUserId(userId, PageRequest.of(page, size));
    }

    public List<TransactionDetail> getTransactionsWithTicketsByUserId(Integer userId, String token) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        return transactions.stream().map(transaction -> {
            TransactionDetail dto = new TransactionDetail();
            dto.setTransactionId(transaction.getTransactionId());
            dto.setEventId(transaction.getEventId());
            dto.setTotalAmount(transaction.getTotalAmount());
            dto.setPaymentMethod(transaction.getPaymentMethod());
            dto.setStatus(transaction.getStatus());
            dto.setTransactionDate(transaction.getTransactionDate().toString());
            dto.setTickets(ticketClient.getTicketsByTransactionId(transaction.getTransactionId(), token));
            return dto;
        }).collect(Collectors.toList());
    }

    public TransactionResponseDto toResponseDto(Transaction transaction) {
        TransactionResponseDto dto = new TransactionResponseDto();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setUserId(transaction.getUserId());
        dto.setEventId(transaction.getEventId());
        dto.setTotalAmount(transaction.getTotalAmount());
        dto.setPaymentMethod(transaction.getPaymentMethod());
        dto.setStatus(transaction.getStatus());
        dto.setTransactionDate(transaction.getTransactionDate());
        return dto;
    }

    public EventClient getEventClient() {
        return eventClient;
    }
}