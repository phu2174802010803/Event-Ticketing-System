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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TicketService {

    @Autowired
    private EventClient eventClient;

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SpacesStorageService spacesStorageService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private KafkaTemplate<String, TicketUpdateEvent> kafkaTemplate;

    @Autowired
    private KafkaTemplate<String, PhaseUpdateEvent> phaseKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, PaymentConfirmationEvent> deadLetterKafkaTemplate;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private static final int MAX_TICKETS_PER_TRANSACTION = 4;
    private static final int HOLD_TIME_MINUTES = 1;
    private static final int MAX_RETRIES = 3;

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
        if (activePhase.getStartTime().isAfter(LocalDateTime.now())
                || activePhase.getEndTime().isBefore(LocalDateTime.now())) {
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

        // Kiểm tra transaction hiện tại của người dùng cho sự kiện này
        String transactionPattern = "transaction:*";
        Set<String> transactionKeys = redisTemplate.keys(transactionPattern);
        String currentTransactionId = null;
        String existingTransactionKey = null;

        for (String key : transactionKeys) {
            String holdKey = redisTemplate.opsForValue().get(key);
            if (holdKey != null && holdKey.contains("ticket:hold:" + userId + ":" + request.getEventId())) {
                currentTransactionId = key.split(":")[1];
                existingTransactionKey = key;
                break;
            }
        }

        boolean isNewTransaction = currentTransactionId == null;
        if (isNewTransaction) {
            // Tạo transactionId mới nếu chưa có
            currentTransactionId = UUID.randomUUID().toString();
        }

        // Kiểm tra vé đã giữ ở khu vực khác và giải phóng nếu cần
        String holdPattern = "ticket:hold:" + userId + ":" + request.getEventId() + ":*";
        Set<String> existingHolds = redisTemplate.keys(holdPattern);
        for (String holdKey : existingHolds) {
            String[] parts = holdKey.split(":");
            Integer heldAreaId = Integer.parseInt(parts[3]);
            if (!heldAreaId.equals(request.getAreaId())) {
                releaseHeldTickets(holdKey); // Giải phóng vé từ khu vực khác
            }
        }

        // Tạo holdKey
        String holdKey = "ticket:hold:" + userId + ":" + request.getEventId() + ":" + request.getAreaId() + ":"
                + request.getPhaseId();
        String transactionKey = "transaction:" + currentTransactionId;

        // Tạm giữ vé trong Redis với định dạng "quantity:price:event_name:area_name"
        String holdValue = request.getQuantity() + ":" + areaDetail.getPrice() + ":" + eventInfo.getName() + ":"
                + areaDetail.getName();

        long ttlInSeconds;
        if (isNewTransaction) {
            // Chỉ set TTL đầy đủ cho transaction mới
            redisTemplate.opsForValue().set(holdKey, holdValue, HOLD_TIME_MINUTES, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(transactionKey, holdKey, HOLD_TIME_MINUTES, TimeUnit.MINUTES);
            ttlInSeconds = HOLD_TIME_MINUTES * 60;

            // Gửi thông tin TTL đến frontend qua WebSocket chỉ cho transaction mới
            messagingTemplate.convertAndSend("/topic/ttl/" + userId,
                    "{\"type\":\"TTL_UPDATE\",\"ttl\":" + ttlInSeconds + "}");

            logger.info("Created new transaction {} for user {} with full TTL", currentTransactionId, userId);
        } else {
            // Với transaction hiện tại, giữ nguyên TTL còn lại
            Long remainingTtl = redisTemplate.getExpire(existingTransactionKey, TimeUnit.SECONDS);
            if (remainingTtl != null && remainingTtl > 0) {
                // Cập nhật holdKey với TTL còn lại
                redisTemplate.opsForValue().set(holdKey, holdValue, remainingTtl, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(transactionKey, holdKey, remainingTtl, TimeUnit.SECONDS);
                ttlInSeconds = remainingTtl;

                logger.info("Updated existing transaction {} for user {} with remaining TTL: {} seconds",
                        currentTransactionId, userId, remainingTtl);
            } else {
                // Fallback nếu không lấy được TTL còn lại
                redisTemplate.opsForValue().set(holdKey, holdValue, HOLD_TIME_MINUTES, TimeUnit.MINUTES);
                redisTemplate.opsForValue().set(transactionKey, holdKey, HOLD_TIME_MINUTES, TimeUnit.MINUTES);
                ttlInSeconds = HOLD_TIME_MINUTES * 60;

                logger.warn("Could not get remaining TTL, using full TTL for transaction {}", currentTransactionId);
            }
        }

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

        // Gửi thông báo cập nhật số vé qua WebSocket
        messagingTemplate.convertAndSend("/topic/tickets/" + request.getEventId() + "/" + request.getAreaId(),
                "{\"availableTickets\": " + newAvailable + "}");

        // Trả về response với event_name và area_name
        TicketSelectionResponse response = new TicketSelectionResponse();
        response.setTransactionId(currentTransactionId);
        response.setEventName(eventInfo.getName());
        response.setAreaName(areaDetail.getName());
        response.setMessage("Chọn vé thành công, vui lòng thanh toán trong " + HOLD_TIME_MINUTES + " phút");
        response.setTtl(ttlInSeconds);
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
        paymentRequest.setPaymentMethod("Thanh toán VNPay");
        paymentRequest.setEventId(eventId);
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);

        // Gửi yêu cầu với RestTemplate
        ResponseEntity<PaymentResponse> responseEntity = restTemplate.exchange(
                paymentServiceUrl + "/api/payments",
                HttpMethod.POST,
                entity,
                PaymentResponse.class);

        PaymentResponse paymentResponse = responseEntity.getBody();
        TicketPurchaseResponse response = new TicketPurchaseResponse();
        response.setPaymentUrl(paymentResponse.getPaymentUrl());
        response.setMessage("Vui lòng thanh toán qua VNPay");
        return response;
    }

    @KafkaListener(topics = "payment-confirmations", groupId = "ticket-service")
    @Transactional
    public void handlePaymentConfirmation(PaymentConfirmationEvent event, Acknowledgment ack) {
        int retryCount = 0;
        boolean success = false;

        while (retryCount < MAX_RETRIES && !success) {
            try {
                processPaymentConfirmation(event);
                success = true;
                ack.acknowledge();
            } catch (Exception e) {
                retryCount++;
                logger.error("Failed to process payment confirmation for transactionId: {}, attempt {}/{}",
                        event.getTransactionId(), retryCount, MAX_RETRIES, e);
                if (retryCount == MAX_RETRIES) {
                    logger.error("Max retries reached, sending to dead-letter queue: {}", event.getTransactionId());
                    deadLetterKafkaTemplate.send("payment-confirmations-dlq", event);
                    ack.acknowledge();
                } else {
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void processPaymentConfirmation(PaymentConfirmationEvent event) {
        String transactionKey = "transaction:" + event.getTransactionId();
        String holdKey = redisTemplate.opsForValue().get(transactionKey);

        if (holdKey == null) {
            logger.warn("No holdKey found for transactionId: {}", event.getTransactionId());
            return;
        }

        String holdData = redisTemplate.opsForValue().get(holdKey);
        if (holdData == null) {
            logger.warn("HoldKey expired for transactionId: {}", event.getTransactionId());
            return;
        }

        String[] holdParts = holdKey.split(":");
        Integer actualEventId = Integer.parseInt(holdParts[3]);
        Integer areaId = Integer.parseInt(holdParts[4]);
        Integer phaseId = Integer.parseInt(holdParts[5]);
        String[] dataParts = holdData.split(":");
        Integer quantity = Integer.parseInt(dataParts[0]);
        Double price = Double.parseDouble(dataParts[1]);
        String eventName = dataParts[2];
        String areaName = dataParts[3];

        String availableKey = "area:available:" + actualEventId + ":" + areaId;
        Integer newAvailable;

        if ("completed".equals(event.getStatus())) {
            for (int i = 0; i < quantity; i++) {
                Ticket ticket = new Ticket();
                ticket.setEventId(actualEventId);
                ticket.setAreaId(areaId);
                ticket.setPhaseId(phaseId);
                ticket.setUserId(event.getUserId());
                ticket.setStatus("sold");
                ticket.setPurchaseDate(LocalDateTime.now());
                ticket.setPrice(price);
                ticket.setTransactionId(event.getTransactionId());
                String qrCodeUrl = generateQRCode(event.getUserId(), actualEventId, areaId);
                ticket.setTicketCode(qrCodeUrl);
                ticketRepository.save(ticket);
            }
            redisTemplate.delete(holdKey);
            redisTemplate.delete(transactionKey);
            newAvailable = Integer.parseInt(redisTemplate.opsForValue().get(availableKey));
            logger.info("Saved {} tickets for userId: {}, eventId: {}", quantity, event.getUserId(), actualEventId);
        } else {
            releaseHeldTickets(holdKey);
            redisTemplate.delete(transactionKey);
            newAvailable = Integer.parseInt(redisTemplate.opsForValue().get(availableKey));
            logger.info("Released tickets for transactionId: {}", event.getTransactionId());
        }

        // Gửi event cập nhật số vé khu vực
        TicketUpdateEvent updateEvent = new TicketUpdateEvent();
        updateEvent.setEventId(actualEventId);
        updateEvent.setAreaId(areaId);
        updateEvent.setAvailableTickets(newAvailable);
        kafkaTemplate.send("ticket-updates", updateEvent);

        // Gửi event cập nhật số vé phiên bán
        try {
            // Lấy thông tin phiên bán từ Event Service
            String token = "system"; // Token hệ thống để gọi internal API
            SellingPhaseResponse[] phases = eventClient.getSellingPhases(actualEventId, token);

            for (SellingPhaseResponse phase : phases) {
                if (phase.getPhaseId().equals(phaseId)) {
                    // Nếu thanh toán thành công - giảm số vé còn lại của phiên
                    // Nếu thanh toán thất bại - không thay đổi (vé sẽ được trả lại tự động)
                    int updatedPhaseTickets = phase.getTicketsAvailable();
                    if ("completed".equals(event.getStatus())) {
                        updatedPhaseTickets = phase.getTicketsAvailable() - quantity;
                    }

                    PhaseUpdateEvent phaseUpdateEvent = new PhaseUpdateEvent();
                    phaseUpdateEvent.setEventId(actualEventId);
                    phaseUpdateEvent.setPhaseId(phaseId);
                    phaseUpdateEvent.setAvailableTickets(updatedPhaseTickets);
                    phaseKafkaTemplate.send("phase-updates", "phaseevent", phaseUpdateEvent);

                    logger.info("Sent phase update event: eventId={}, phaseId={}, availableTickets={}",
                            actualEventId, phaseId, updatedPhaseTickets);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send phase update event for eventId: {}, phaseId: {}", actualEventId, phaseId, e);
        }

        messagingTemplate.convertAndSend("/topic/tickets/" + actualEventId + "/" + areaId,
                new TicketUpdateResponse(areaId, newAvailable.toString(), "Ticket update successful"));
    }

    @Transactional(readOnly = true)
    public EventSalesResponseDto getEventSalesForOrganizer(Integer eventId, Integer organizerId, String token) {
        EventInfo eventInfo = eventClient.getEventInfo(eventId, token);
        if (eventInfo == null || !eventClient.checkEventOwnership(organizerId, eventId, token)) {
            throw new IllegalArgumentException("Không tìm thấy sự kiện hoặc bạn không có quyền truy cập");
        }

        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        int soldTickets = (int) tickets.stream()
                .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                .count();
        double totalRevenue = tickets.stream()
                .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                .mapToDouble(Ticket::getPrice)
                .sum();

        List<AreaResponseDto> areas = eventClient.getAreasByEventForOrganizer(eventId, token);
        int totalTickets = areas.stream().mapToInt(AreaResponseDto::getTotalTickets).sum();
        int availableTickets = totalTickets - soldTickets;

        List<AreaSalesDto> areaSales = areas.stream().map(area -> {
            int areaSoldTickets = (int) tickets.stream()
                    .filter(t -> t.getAreaId().equals(area.getAreaId()) &&
                            ("sold".equals(t.getStatus()) || "used".equals(t.getStatus())))
                    .count();
            AreaSalesDto dto = new AreaSalesDto();
            dto.setAreaId(area.getAreaId());
            dto.setAreaName(area.getName());
            dto.setTotalTickets(area.getTotalTickets());
            dto.setSoldTickets(areaSoldTickets);
            dto.setAvailableTickets(area.getTotalTickets() - areaSoldTickets);
            dto.setPrice(area.getPrice());
            return dto;
        }).collect(Collectors.toList());

        List<PhaseSalesDto> phaseSales = getPhaseSalesForEvent(eventId, tickets, token);

        String cacheKey = "sales:organizer:" + eventId;
        redisTemplate.opsForValue().set(cacheKey, String.format("%d:%d:%.2f", soldTickets, totalTickets, totalRevenue),
                5, TimeUnit.MINUTES);

        EventSalesResponseDto response = new EventSalesResponseDto();
        response.setEventId(eventId);
        response.setEventName(eventInfo.getName());
        response.setTotalTickets(totalTickets);
        response.setSoldTickets(soldTickets);
        response.setAvailableTickets(availableTickets);
        response.setTotalRevenue(totalRevenue);
        response.setAreas(areaSales);
        response.setPhases(phaseSales);
        return response;
    }

    @Transactional(readOnly = true)
    public EventSalesResponseDto getEventSalesForAdmin(Integer eventId, String token) {
        EventInfo eventInfo = eventClient.getEventInfo(eventId, token);
        if (eventInfo == null) {
            throw new IllegalArgumentException("Không tìm thấy sự kiện");
        }

        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        int soldTickets = (int) tickets.stream()
                .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                .count();
        double totalRevenue = tickets.stream()
                .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                .mapToDouble(Ticket::getPrice)
                .sum();

        List<AreaResponseDto> areas = eventClient.getAreasByEventForAdmin(eventId, token);
        int totalTickets = areas.stream().mapToInt(AreaResponseDto::getTotalTickets).sum();
        int availableTickets = totalTickets - soldTickets;

        List<AreaSalesDto> areaSales = areas.stream().map(area -> {
            int areaSoldTickets = (int) tickets.stream()
                    .filter(t -> t.getAreaId().equals(area.getAreaId()) &&
                            ("sold".equals(t.getStatus()) || "used".equals(t.getStatus())))
                    .count();
            AreaSalesDto dto = new AreaSalesDto();
            dto.setAreaId(area.getAreaId());
            dto.setAreaName(area.getName());
            dto.setTotalTickets(area.getTotalTickets());
            dto.setSoldTickets(areaSoldTickets);
            dto.setAvailableTickets(area.getTotalTickets() - areaSoldTickets);
            dto.setPrice(area.getPrice());
            return dto;
        }).collect(Collectors.toList());

        List<PhaseSalesDto> phaseSales = getPhaseSalesForEvent(eventId, tickets, token);

        String cacheKey = "sales:admin:event:" + eventId;
        redisTemplate.opsForValue().set(cacheKey, String.format("%d:%d:%.2f", soldTickets, totalTickets, totalRevenue),
                5, TimeUnit.MINUTES);

        EventSalesResponseDto response = new EventSalesResponseDto();
        response.setEventId(eventId);
        response.setEventName(eventInfo.getName());
        response.setTotalTickets(totalTickets);
        response.setSoldTickets(soldTickets);
        response.setAvailableTickets(availableTickets);
        response.setTotalRevenue(totalRevenue);
        response.setAreas(areaSales);
        response.setPhases(phaseSales);
        return response;
    }

    private List<PhaseSalesDto> getPhaseSalesForEvent(Integer eventId, List<Ticket> tickets, String token) {
        SellingPhaseResponse[] phases = eventClient.getSellingPhases(eventId, token);
        return Arrays.stream(phases).map(phase -> {
            int phaseSoldTickets = (int) tickets.stream()
                    .filter(t -> t.getPhaseId() != null && t.getPhaseId().equals(phase.getPhaseId()) &&
                            ("sold".equals(t.getStatus()) || "used".equals(t.getStatus())))
                    .count();
            double phaseRevenue = tickets.stream()
                    .filter(t -> t.getPhaseId() != null && t.getPhaseId().equals(phase.getPhaseId()) &&
                            ("sold".equals(t.getStatus()) || "used".equals(t.getStatus())))
                    .mapToDouble(Ticket::getPrice)
                    .sum();
            PhaseSalesDto dto = new PhaseSalesDto();
            dto.setPhaseId(phase.getPhaseId());
            dto.setStartTime(phase.getStartTime());
            dto.setEndTime(phase.getEndTime());
            dto.setSoldTickets(phaseSoldTickets);
            dto.setRevenue(phaseRevenue);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemSalesResponseDto getSystemSales(int page, int size, String token) {
        // Fetch all events from Event Service
        List<EventInfo> allEvents = eventClient.getAllEvents(token);
        int totalEvents = allEvents.size();

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, totalEvents);
        List<EventInfo> pagedEvents = allEvents.subList(start, end);

        // Fetch all tickets
        List<Ticket> allTickets = ticketRepository.findAll();
        int totalSoldTickets = (int) allTickets.stream()
                .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                .count();
        double totalRevenue = allTickets.stream()
                .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                .mapToDouble(Ticket::getPrice)
                .sum();

        // Build event summaries
        List<EventSalesSummaryDto> eventSummaries = pagedEvents.stream().map(event -> {
            List<Ticket> eventTickets = allTickets.stream()
                    .filter(t -> t.getEventId().equals(event.getEventId()))
                    .collect(Collectors.toList());
            int sold = (int) eventTickets.stream()
                    .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                    .count();
            double revenue = eventTickets.stream()
                    .filter(t -> "sold".equals(t.getStatus()) || "used".equals(t.getStatus()))
                    .mapToDouble(Ticket::getPrice)
                    .sum();
            EventSalesSummaryDto summary = new EventSalesSummaryDto();
            summary.setEventId(event.getEventId());
            summary.setEventName(event.getName());
            summary.setSoldTickets(sold);
            summary.setTotalRevenue(revenue);
            return summary;
        }).collect(Collectors.toList());

        // Cache system-wide stats
        String cacheKey = "sales:admin:page" + page + ":size" + size;
        redisTemplate.opsForValue().set(cacheKey, String.format("%d:%.2f", totalSoldTickets, totalRevenue), 5,
                java.util.concurrent.TimeUnit.MINUTES);

        // Build response
        SystemSalesResponseDto response = new SystemSalesResponseDto();
        response.setTotalEvents(totalEvents);
        response.setTotalSoldTickets(totalSoldTickets);
        response.setTotalRevenue(totalRevenue);
        response.setEvents(eventSummaries);
        response.setPage(page);
        response.setSize(size);
        return response;
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

    @Transactional(readOnly = true)
    public List<TicketResponse> getUserTickets(Integer userId, String status, int page, int size, String token) {
        Page<Ticket> tickets = status == null ? ticketRepository.findByUserId(userId, PageRequest.of(page, size))
                : ticketRepository.findByUserIdAndStatus(userId, status, PageRequest.of(page, size));
        return enrichTicketData(tickets, token, "USER");
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getOrganizerTickets(Integer organizerId, Integer eventId, String status, int page,
                                                    int size, String token) {
        if (!eventClient.checkEventOwnership(organizerId, eventId, token)) {
            throw new IllegalStateException("Bạn không có quyền truy cập danh sách vé của sự kiện này");
        }
        Page<Ticket> tickets = status == null ? ticketRepository.findByEventId(eventId, PageRequest.of(page, size))
                : ticketRepository.findByEventIdAndStatus(eventId, status, PageRequest.of(page, size));
        return enrichTicketData(tickets, token, "ORGANIZER");
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAdminTickets(Integer eventId, String status, int page, int size, String token) {
        Page<Ticket> tickets = eventId != null
                ? (status == null ? ticketRepository.findByEventId(eventId, PageRequest.of(page, size))
                : ticketRepository.findByEventIdAndStatus(eventId, status, PageRequest.of(page, size)))
                : (status == null ? ticketRepository.findAll(PageRequest.of(page, size))
                : ticketRepository.findByStatus(status, PageRequest.of(page, size)));
        return enrichTicketData(tickets, token, "ADMIN");
    }

    public List<TicketDetail> getTicketsByTransactionId(String transactionId, String token) {
        List<Ticket> tickets = ticketRepository.findByTransactionId(transactionId);
        return tickets.stream().map(ticket -> {
            AreaDetailDto areaDetail = eventClient.getAreaDetail(ticket.getEventId(), ticket.getAreaId(), token);
            EventInfo eventInfo = eventClient.getEventInfo(ticket.getEventId(), token);

            SellingPhaseResponse phase = ticket.getPhaseId() != null
                    ? fetchSellingPhase(ticket.getEventId(), ticket.getPhaseId(), token)
                    : null;

            TicketDetail dto = new TicketDetail();
            dto.setTicketId(ticket.getTicketId());
            dto.setTicketCode(ticket.getTicketCode());
            dto.setStatus(ticket.getStatus());
            dto.setPurchaseDate(ticket.getPurchaseDate() != null ? ticket.getPurchaseDate().toString() : null);
            dto.setPrice(ticket.getPrice());
            dto.setEventName(eventInfo != null ? eventInfo.getName() : "Sự kiện không xác định");
            dto.setAreaName(areaDetail != null ? areaDetail.getName() : "Khu vực không xác định");
            dto.setPhaseStartTime(phase != null ? phase.getStartTime().toString() : null);
            dto.setPhaseEndTime(phase != null ? phase.getEndTime().toString() : null);

            return dto;
        }).collect(Collectors.toList());
    }

    private List<TicketResponse> enrichTicketData(Page<Ticket> tickets, String token, String role) {
        return tickets.stream().map(ticket -> {
            EventPublicDetailDto eventDetail = fetchEventDetail(ticket.getEventId(), token);
            AreaResponseDto areaDetail = fetchAreaDetail(ticket.getEventId(), ticket.getAreaId(), token);
            TransactionResponseDto transactionDetail = fetchTransactionDetail(ticket.getTransactionId(), token);

            UserResponseDto user = null;
            if (!"USER".equals(role)) {
                user = fetchUserDetail(ticket.getUserId(), token);
            }

            SellingPhaseResponse phase = ticket.getPhaseId() != null
                    ? fetchSellingPhase(ticket.getEventId(), ticket.getPhaseId(), token)
                    : null;

            TicketResponse response = new TicketResponse();
            response.setTicketCode(ticket.getTicketCode());
            response.setStatus(ticket.getStatus());
            response.setPurchaseDate(ticket.getPurchaseDate() != null ? ticket.getPurchaseDate().toString() : null);
            response.setPrice(ticket.getPrice());
            response.setTransactionId(ticket.getTransactionId());
            response.setEventName(eventDetail != null ? eventDetail.getName() : "Sự kiện không xác định");
            response.setAreaName(areaDetail != null ? areaDetail.getName() : "Khu vực không xác định");
            response.setPhaseStartTime(phase != null ? phase.getStartTime().toString() : null);
            response.setPhaseEndTime(phase != null ? phase.getEndTime().toString() : null);

            if (eventDetail != null) {
                // Kiểm tra và set event date và time
                if (eventDetail.getDate() != null) {
                    response.setEventDate(eventDetail.getDate().toString());
                }
                if (eventDetail.getTime() != null) {
                    response.setEventTime(eventDetail.getTime().toString());
                }
                response.setEventLocation(eventDetail.getLocation());
            }
            if (transactionDetail != null) {
                response.setPaymentMethod(transactionDetail.getPaymentMethod());
            }

            if (!"USER".equals(role)) {
                response.setUserFullName(user != null ? user.getFullName() : "Người dùng không xác định");
                response.setUserEmail(user != null ? user.getEmail() : "Email không xác định");
            }

            if ("ADMIN".equals(role)) {
                response.setTicketId(ticket.getTicketId());
                response.setEventId(ticket.getEventId());
                response.setAreaId(ticket.getAreaId());
                response.setPhaseId(ticket.getPhaseId());
                response.setUserId(ticket.getUserId());
            }

            return response;
        }).collect(Collectors.toList());
    }

    private EventPublicDetailDto fetchEventDetail(Integer eventId, String token) {
        try {
            return eventClient.getPublicEventDetail(eventId, token);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thông tin sự kiện {}: {}", eventId, e.getMessage());
            return null;
        }
    }

    private AreaResponseDto fetchAreaDetail(Integer eventId, Integer areaId, String token) {
        try {
            AreaDetailDto areaDetail = eventClient.getAreaDetail(eventId, areaId, token);
            if (areaDetail == null) {
                return null;
            }

            // Convert AreaDetailDto to AreaResponseDto
            AreaResponseDto areaResponse = new AreaResponseDto();
            areaResponse.setAreaId(areaDetail.getAreaId());
            areaResponse.setEventId(eventId);
            areaResponse.setName(areaDetail.getName());
            areaResponse.setTotalTickets(areaDetail.getTotalTickets());
            areaResponse.setAvailableTickets(areaDetail.getAvailableTickets());
            areaResponse.setPrice(areaDetail.getPrice());

            return areaResponse;
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thông tin khu vực {}: {}", areaId, e.getMessage());
            return null;
        }
    }

    private UserResponseDto fetchUserDetail(Integer userId, String token) {
        try {
            return identityClient.getUserDetail(userId, token);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thông tin người dùng {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private SellingPhaseResponse fetchSellingPhase(Integer eventId, Integer phaseId, String token) {
        SellingPhaseResponse[] phases = eventClient.getSellingPhases(eventId, token);
        return Arrays.stream(phases)
                .filter(p -> p.getPhaseId().equals(phaseId))
                .findFirst()
                .orElse(null);
    }

    private TransactionResponseDto fetchTransactionDetail(String transactionId, String token) {
        try {
            return paymentClient.getTransactionDetail(transactionId, token);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thông tin giao dịch {}: {}", transactionId, e.getMessage());
            return null;
        }
    }

    public void releaseHeldTickets(String holdKey) {
        String holdData = redisTemplate.opsForValue().get(holdKey);
        if (holdData != null) {
            String[] parts = holdKey.split(":");
            Integer eventId = Integer.parseInt(parts[3]);
            Integer areaId = Integer.parseInt(parts[4]);
            Integer phaseId = parts.length > 5 ? Integer.parseInt(parts[5]) : null;
            Integer quantity = Integer.parseInt(holdData.split(":")[0]);
            String availableKey = "area:available:" + eventId + ":" + areaId;
            Long newAvailable = redisTemplate.opsForValue().increment(availableKey, quantity);
            redisTemplate.delete(holdKey);

            // Gửi sự kiện cập nhật số vé khu vực đến Kafka
            TicketUpdateEvent event = new TicketUpdateEvent();
            event.setEventId(eventId);
            event.setAreaId(areaId);
            event.setAvailableTickets(newAvailable.intValue());
            kafkaTemplate.send("ticket-updates", "ticketevent", event);
            logger.info("Đã gửi sự kiện giải phóng vé khu vực: eventId={}, areaId={}, availableTickets={}", eventId,
                    areaId,
                    newAvailable);

            // Gửi sự kiện cập nhật số vé phiên bán (nếu có phaseId)
            if (phaseId != null) {
                try {
                    String token = "system";
                    SellingPhaseResponse[] phases = eventClient.getSellingPhases(eventId, token);
                    for (SellingPhaseResponse phase : phases) {
                        if (phase.getPhaseId().equals(phaseId)) {
                            // Khi giải phóng vé, trả lại số vé cho phiên
                            int updatedPhaseTickets = phase.getTicketsAvailable() + quantity;

                            PhaseUpdateEvent phaseUpdateEvent = new PhaseUpdateEvent();
                            phaseUpdateEvent.setEventId(eventId);
                            phaseUpdateEvent.setPhaseId(phaseId);
                            phaseUpdateEvent.setAvailableTickets(updatedPhaseTickets);
                            phaseKafkaTemplate.send("phase-updates", "phaseevent", phaseUpdateEvent);

                            logger.info(
                                    "Đã gửi sự kiện giải phóng vé phiên: eventId={}, phaseId={}, availableTickets={}",
                                    eventId, phaseId, updatedPhaseTickets);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to send phase release event for eventId: {}, phaseId: {}", eventId, phaseId,
                            e);
                }
            }

            // Gửi thông báo qua WebSocket
            messagingTemplate.convertAndSend("/topic/tickets/" + eventId + "/" + areaId,
                    "{\"availableTickets\": " + newAvailable + "}");
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
            return spacesStorageService.uploadQRCode(qrCodeBytes, fileName);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Không thể tạo mã QR: " + e.getMessage());
        }
    }
}