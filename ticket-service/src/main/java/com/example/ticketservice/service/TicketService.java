package com.example.ticketservice.service;

import com.example.ticketservice.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TicketService {

    @Autowired
    private EventClient eventClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

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


        // Tạm giữ vé trong Redis
        String holdKey = "ticket:hold:" + userId + ":" + request.getEventId() + ":" + request.getAreaId();
        redisTemplate.opsForValue().set(holdKey, String.valueOf(request.getQuantity()), HOLD_TIME_MINUTES, TimeUnit.MINUTES);

        // Giảm số vé tạm thời
        String availableKey = "area:available:" + request.getEventId() + ":" + request.getAreaId();
        redisTemplate.opsForValue().setIfAbsent(availableKey, String.valueOf(areaDetail.getAvailableTickets()));
        Long newAvailable = redisTemplate.opsForValue().increment(availableKey, -request.getQuantity());
        if (newAvailable < 0) {
            redisTemplate.opsForValue().increment(availableKey, request.getQuantity());
            redisTemplate.delete(holdKey);
            throw new IllegalArgumentException("Không đủ vé do người dùng khác đã chọn");
        }

        // Tạo transactionId
        String transactionId = UUID.randomUUID().toString();
        TicketSelectionResponse response = new TicketSelectionResponse();
        response.setTransactionId(transactionId);
        response.setMessage("Chọn vé thành công, vui lòng thanh toán trong " + HOLD_TIME_MINUTES + " phút");
        return response;
    }

    public TicketPurchaseResponse purchaseTickets(TicketPurchaseRequest request, Integer userId, String token) {
        String holdPattern = "ticket:hold:" + userId + ":*";
        Set<String> holdKeys = redisTemplate.keys(holdPattern);
        if (holdKeys.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy vé tạm giữ để thanh toán");
        }

        String holdKey = holdKeys.iterator().next();
        String quantityStr = redisTemplate.opsForValue().get(holdKey);
        if (quantityStr == null) {
            throw new IllegalArgumentException("Phiên tạm giữ vé đã hết hạn");
        }

        String[] parts = holdKey.split(":");
        Integer eventId = Integer.parseInt(parts[2]);
        Integer areaId = Integer.parseInt(parts[3]);
        Integer quantity = Integer.parseInt(quantityStr);

        //Bổ sung logic thanh toán ở đây (payment service)

        // Giả lập thanh toán thành công
        String ticketCode = UUID.randomUUID().toString();
        redisTemplate.delete(holdKey);

        TicketPurchaseResponse response = new TicketPurchaseResponse();
        response.setTicketCode(ticketCode);
        response.setMessage("Thanh toán thành công, mã vé của bạn là " + ticketCode);
        return response;
    }

    // Giải phóng vé khi hết thời gian hoặc thanh toán thất bại
    public void releaseHeldTickets(String holdKey) {
        String quantityStr = redisTemplate.opsForValue().get(holdKey);
        if (quantityStr != null) {
            int quantity = Integer.parseInt(quantityStr);
            String[] parts = holdKey.split(":");
            Integer eventId = Integer.parseInt(parts[2]);
            Integer areaId = Integer.parseInt(parts[3]);
            String availableKey = "area:available:" + eventId + ":" + areaId;
            redisTemplate.opsForValue().increment(availableKey, quantity);
            redisTemplate.delete(holdKey);
        }
    }
}