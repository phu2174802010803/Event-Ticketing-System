package com.example.ticketservice.service;

import com.example.ticketservice.dto.TicketSelectionRequest;
import com.example.ticketservice.dto.TicketSelectionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TicketService {

    @Autowired
    private EventClient eventClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public TicketSelectionResponse selectTickets(TicketSelectionRequest request, Integer userId, String token) {
        String activeKey = "active:" + request.getEventId();
        Double score = redisTemplate.opsForZSet().score(activeKey, userId.toString());
        if (score == null) {
            throw new RuntimeException("Bạn cần tham gia hàng đợi trước khi chọn vé");
        }

        var areaDetail = eventClient.getAreaDetail(request.getEventId(), request.getAreaId(), token);
        if (areaDetail == null) {
            throw new RuntimeException("Không tìm thấy khu vực");
        }

        if (areaDetail.getAvailableTickets() < request.getQuantity()) {
            throw new RuntimeException("Không đủ vé trong khu vực");
        }

        String transactionId = UUID.randomUUID().toString();
        String holdKey = "ticket:hold:" + userId + ":" + request.getEventId() + ":" + request.getAreaId();
        redisTemplate.opsForValue().set(holdKey, String.valueOf(request.getQuantity()), 10, TimeUnit.MINUTES);

        TicketSelectionResponse response = new TicketSelectionResponse();
        response.setTransactionId(transactionId);
        response.setMessage("Chọn vé thành công, vui lòng thanh toán trong 10 phút");
        return response;
    }
}