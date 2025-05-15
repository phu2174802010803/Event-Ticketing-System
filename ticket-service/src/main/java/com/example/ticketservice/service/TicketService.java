package com.example.ticketservice.service;

import com.example.ticketservice.dto.*;
import com.example.ticketservice.model.Ticket;
import com.example.ticketservice.repository.TicketRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TicketService {

    @Autowired
    private EventClient eventClient;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private static final int MAX_TICKETS_PER_TRANSACTION = 4;
    private static final int HOLD_TIME_MINUTES = 10;

    public TicketSelectionResponse selectTickets(TicketSelectionRequest request, Integer userId, String token) {
        // Kiểm tra hàng đợi
        String activeKey = "active:" + request.getEventId();
        Double score = redisTemplate.opsForZSet().score(activeKey, userId.toString());
        if (score == null) {
            throw new IllegalArgumentException("Bạn cần tham gia hàng đợi trước khi chọn vé");
        }

        AreaDetailDto areaDetail = eventClient.getAreaDetail(request.getEventId(), request.getAreaId(), token);
        if (areaDetail == null) {
            throw new IllegalArgumentException("Không tìm thấy khu vực");
        }

        // Lấy thông tin sự kiện từ Event Service
        EventInfo eventInfo = eventClient.getEventInfo(request.getEventId(), token);
        if (eventInfo == null) {
            throw new IllegalArgumentException("Không tìm thấy sự kiện");
        }

        // Kiểm tra phiên bán vé
        SellingPhaseResponse[] phases = eventClient.getSellingPhases(request.getEventId(), token);
        SellingPhaseResponse activePhase = null;
        for (SellingPhaseResponse phase : phases) {
            if (phase.getPhaseId().equals(request.getPhaseId()) &&
                    (phase.getAreaId() == null || phase.getAreaId().equals(request.getAreaId()))) {
                activePhase = phase;
                break;
            }
        }
        if (activePhase == null) {
            throw new IllegalArgumentException("Phiên bán vé không tồn tại hoặc không áp dụng cho khu vực này");
        }
        if (activePhase.getStartTime().isAfter(LocalDateTime.now()) || activePhase.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Phiên bán vé không trong thời gian hoạt động");
        }
        if (activePhase.getTicketsAvailable() < request.getQuantity()) {
            throw new IllegalArgumentException("Không đủ vé trong phiên bán vé này");
        }
        if (areaDetail.getAvailableTickets() < request.getQuantity()) {
            throw new IllegalArgumentException("Không đủ vé trong khu vực");
        }
        if (request.getQuantity() > MAX_TICKETS_PER_TRANSACTION) {
            throw new IllegalArgumentException("Số lượng vé tối đa là " + MAX_TICKETS_PER_TRANSACTION);
        }

        // Kiểm tra vé đã giữ ở khu vực khác
        String holdPattern = "ticket:hold:" + userId + ":" + request.getEventId() + ":*";
        Set<String> existingHolds = redisTemplate.keys(holdPattern);
        if (!existingHolds.isEmpty()) {
            for (String holdKey : existingHolds) {
                String[] parts = holdKey.split(":");
                Integer heldAreaId = Integer.parseInt(parts[3]);
                if (!heldAreaId.equals(request.getAreaId())) {
                    throw new IllegalArgumentException("Bạn chỉ được chọn vé từ một khu vực trong một giao dịch");
                }
            }
        }

        // Tạo holdKey và transactionId
        String holdKey = "ticket:hold:" + userId + ":" + request.getEventId() + ":" + request.getAreaId() + ":" + request.getPhaseId();
        String transactionId = UUID.randomUUID().toString();
        String transactionKey = "transaction:" + transactionId;

        // Tạm giữ vé trong Redis với định dạng "quantity:price:event_name:area_name"
        String holdValue = request.getQuantity() + ":" + areaDetail.getPrice() + ":" + eventInfo.getName() + ":" + areaDetail.getName(); //quantity:price:event_name:area_name
        redisTemplate.opsForValue().set(holdKey, holdValue, HOLD_TIME_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(transactionKey, holdKey, HOLD_TIME_MINUTES, TimeUnit.MINUTES);

        // Giảm số vé tạm thời
        String availableKey = "area:available:" + request.getEventId() + ":" + request.getAreaId();
        redisTemplate.opsForValue().setIfAbsent(availableKey, String.valueOf(areaDetail.getAvailableTickets()));
        Long newAvailable = redisTemplate.opsForValue().increment(availableKey, -request.getQuantity());
        if (newAvailable < 0) {
            redisTemplate.opsForValue().increment(availableKey, request.getQuantity());
            redisTemplate.delete(holdKey);
            redisTemplate.delete(transactionKey);
            throw new IllegalArgumentException("Không đủ vé do người dùng khác đã chọn");
        }

        // Trả về response với event_name và area_name
        TicketSelectionResponse response = new TicketSelectionResponse();
        response.setTransactionId(transactionId);
        response.setEventName(eventInfo.getName());
        response.setAreaName(areaDetail.getName());
        response.setMessage("Chọn vé thành công, vui lòng thanh toán trong " + HOLD_TIME_MINUTES + " phút");
        return response;
    }

    @Transactional
    public TicketPurchaseResponse purchaseTickets(TicketPurchaseRequest request, Integer userId, String token) {
        String holdPattern = "ticket:hold:" + userId + ":*";
        Set<String> holdKeys = redisTemplate.keys(holdPattern);
        if (holdKeys.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy vé tạm giữ để thanh toán");
        }

        String holdKey = holdKeys.iterator().next();
        String holdData = redisTemplate.opsForValue().get(holdKey);
        if (holdData == null) {
            throw new IllegalArgumentException("Phiên tạm giữ vé đã hết hạn");
        }

        String[] parts = holdKey.split(":");
        Integer eventId = Integer.parseInt(parts[3]); // "ticket:hold:<userId>:<eventId>:<areaId>:<phaseId>"
        Integer quantity = Integer.parseInt(holdData.split(":")[0]);
        Double price = Double.parseDouble(holdData.split(":")[1]);
        Double totalAmount = quantity * price;

        // Gửi yêu cầu thanh toán đến Payment Service
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setTransactionId(request.getTransactionId());
        paymentRequest.setAmount(totalAmount);
        paymentRequest.setPaymentMethod("bank");
        paymentRequest.setEventId(eventId);
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);

        // Gửi yêu cầu với RestTemplate
        ResponseEntity<PaymentResponse> responseEntity = restTemplate.exchange(
                paymentServiceUrl + "/api/payments",
                HttpMethod.POST,
                entity,
                PaymentResponse.class
        );

        PaymentResponse paymentResponse = responseEntity.getBody();
        TicketPurchaseResponse response = new TicketPurchaseResponse();
        response.setPaymentUrl(paymentResponse.getPaymentUrl());
        response.setMessage("Vui lòng thanh toán qua VNPay");
        return response;
    }

    @Transactional
    public void confirmPayment(String transactionId, String status, Integer userId, Integer eventId) {
        // Lấy holdKey từ transactionId
        String transactionKey = "transaction:" + transactionId;
        String holdKey = redisTemplate.opsForValue().get(transactionKey);

        if (holdKey == null) {
            logger.warn("Không tìm thấy holdKey cho transactionId: {}", transactionId);
            return;
        }

        String holdData = redisTemplate.opsForValue().get(holdKey);
        if (holdData == null) {
            logger.warn("HoldKey đã hết hạn cho transactionId: {}", transactionId);
            return;
        }

        // Trích xuất thông tin từ holdData và holdKey
        String[] holdParts = holdKey.split(":");
        Integer actualEventId = Integer.parseInt(holdParts[3]);
        Integer areaId = Integer.parseInt(holdParts[4]);
        Integer phaseId = Integer.parseInt(holdParts[5]);
        String[] dataParts = holdData.split(":");
        Integer quantity = Integer.parseInt(dataParts[0]);
        Double price = Double.parseDouble(dataParts[1]);
        String eventName = dataParts[2];
        String areaName = dataParts[3];

        if ("completed".equals(status)) {
            for (int i = 0; i < quantity; i++) {
                Ticket ticket = new Ticket();
                ticket.setEventId(actualEventId);
                ticket.setAreaId(areaId);
                ticket.setPhaseId(phaseId);
                ticket.setUserId(userId);
                ticket.setStatus("sold");
                ticket.setPurchaseDate(LocalDateTime.now());
                ticket.setPrice(price);
                ticket.setEventName(eventName); // Lưu event_name
                ticket.setAreaName(areaName);   // Lưu area_name
                String qrCodeUrl = generateQRCode(userId, actualEventId, areaId);
                ticket.setTicketCode(qrCodeUrl);
                ticketRepository.save(ticket);
            }
            redisTemplate.delete(holdKey);
            redisTemplate.delete(transactionKey);
            logger.info("Đã lưu {} vé cho userId: {}, eventId: {}", quantity, userId, actualEventId);
        } else {
            releaseHeldTickets(holdKey);
            redisTemplate.delete(transactionKey);
            logger.info("Đã giải phóng vé cho transactionId: {}", transactionId);
        }

        // Cập nhật số vé khả dụng trong Redis
        String availableKey = "area:available:" + actualEventId + ":" + areaId;
        String availableTickets = redisTemplate.opsForValue().get(availableKey);
        messagingTemplate.convertAndSend("/topic/tickets/" + actualEventId + "/" + areaId,
                new TicketUpdateResponse(areaId, availableTickets, "Cập nhật số vé thành công"));
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getTicketHistory(Integer userId, String status, int page, int size, String role) {
        if (!"USER".equals(role)) {
            throw new IllegalStateException("Chỉ người dùng mới có quyền truy cập lịch sử vé cá nhân");
        }
        Page<Ticket> tickets = ticketRepository.findByUserIdAndStatus(userId, status, PageRequest.of(page, size));
        return tickets.stream()
                .map(ticket -> new TicketHistoryResponse(
                        ticket.getTicketId(),
                        ticket.getEventName(),
                        ticket.getAreaName(),
                        ticket.getStatus(),
                        ticket.getTicketCode(),
                        ticket.getPurchaseDate().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getTicketHistoryForOrganizer(Integer organizerId, Integer eventId, String status, int page, int size) {
        // Kiểm tra quyền sở hữu sự kiện qua Event Service (giả định)
        // eventClient.checkEventOwnership(organizerId, eventId);
        Page<Ticket> tickets = ticketRepository.findByEventIdAndStatus(eventId, status, PageRequest.of(page, size));
        return tickets.stream()
                .map(ticket -> new TicketHistoryResponse(
                        ticket.getTicketId(),
                        ticket.getEventName(),
                        ticket.getAreaName(),
                        ticket.getStatus(),
                        ticket.getTicketCode(),
                        ticket.getPurchaseDate().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getAllTickets(Integer eventId, String status, int page, int size, String role) {
        if (!"ADMIN".equals(role)) {
            throw new IllegalStateException("Chỉ Admin mới có quyền truy cập tất cả vé");
        }
        Page<Ticket> tickets = eventId != null ?
                ticketRepository.findByEventIdAndStatus(eventId, status, PageRequest.of(page, size)) :
                ticketRepository.findAll(PageRequest.of(page, size));
        return tickets.stream()
                .map(ticket -> new TicketHistoryResponse(
                        ticket.getTicketId(),
                        ticket.getEventName(),
                        ticket.getAreaName(),
                        ticket.getStatus(),
                        ticket.getTicketCode(),
                        ticket.getPurchaseDate().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketQRResponse getTicketQR(Integer ticketId, Integer userId, String role) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Vé không tồn tại"));
        if ("USER".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Vé không thuộc về bạn");
        }
        // Organizer/Admin không cần kiểm tra quyền sở hữu vé, chỉ cần vé tồn tại
        return new TicketQRResponse(ticket.getTicketCode());
    }

    @Transactional
    public TicketScanResponse scanTicket(TicketScanRequest request, String role) {
        if (!"ORGANIZER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("Chỉ Organizer hoặc Admin mới có quyền quét vé");
        }
        Ticket ticket = ticketRepository.findByTicketCode(request.getQrCode())
                .orElseThrow(() -> new IllegalArgumentException("Mã QR không hợp lệ"));
        if ("used".equals(ticket.getStatus())) {
            return new TicketScanResponse("Vé đã được sử dụng", "used");
        }
        ticket.setStatus("used");
        ticketRepository.save(ticket);
        return new TicketScanResponse("Vé hợp lệ", "used");
    }

    public void releaseHeldTickets(String holdKey) {
        String holdData = redisTemplate.opsForValue().get(holdKey);
        if (holdData != null) {
            String[] parts = holdKey.split(":");
            Integer eventId = Integer.parseInt(parts[3]);
            Integer areaId = Integer.parseInt(parts[4]);
            Integer quantity = Integer.parseInt(holdData.split(":")[0]);
            String availableKey = "area:available:" + eventId + ":" + areaId;
            redisTemplate.opsForValue().increment(availableKey, quantity);
            redisTemplate.delete(holdKey);
            messagingTemplate.convertAndSend("/topic/tickets/" + eventId + "/" + areaId,
                    new TicketUpdateResponse(areaId, redisTemplate.opsForValue().get(availableKey), "Vé đã được giải phóng"));
        }
    }

    public String generateQRCode(Integer userId, Integer eventId, Integer areaId) {
        String qrData = userId + ":" + eventId + ":" + areaId + ":" + System.currentTimeMillis();
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] qrCodeBytes = pngOutputStream.toByteArray();
            String fileName = "qr_" + UUID.randomUUID().toString() + ".png";
            return azureBlobStorageService.uploadQRCode(qrCodeBytes, fileName);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Không thể tạo mã QR: " + e.getMessage());
        }
    }
}