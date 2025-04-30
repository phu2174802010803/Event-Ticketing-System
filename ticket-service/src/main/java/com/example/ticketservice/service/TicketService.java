package com.example.ticketservice.service;

import com.example.ticketservice.dto.AreaDetailDto;
import com.example.ticketservice.dto.TicketPurchaseRequest;
import com.example.ticketservice.dto.TicketPurchaseResponse;
import com.example.ticketservice.dto.TicketSelectionRequest;
import com.example.ticketservice.dto.TicketSelectionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;



import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TicketService {

    @Autowired
    private EventClient eventClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final int MAX_TICKETS_PER_TRANSACTION = 4;
    private static final int HOLD_TIME_MINUTES = 10;

    public TicketSelectionResponse selectTickets(TicketSelectionRequest request, Integer userId, String token) {
        // Kiểm tra hàng đợi
        String activeKey = "active:" + request.getEventId();
        Double score = redisTemplate.opsForZSet().score(activeKey, userId.toString());
        if (score == null) {
            throw new RuntimeException("Bạn cần tham gia hàng đợi trước khi chọn vé");
        }

        // Kiểm tra số lượng vé tối đa
        if (request.getQuantity() > MAX_TICKETS_PER_TRANSACTION) {
            throw new RuntimeException("Số lượng vé tối đa là " + MAX_TICKETS_PER_TRANSACTION);
        }

        // Lấy thông tin khu vực từ event-service
        AreaDetailDto areaDetail = eventClient.getAreaDetail(request.getEventId(), request.getAreaId(), token);
        if (areaDetail == null) {
            throw new RuntimeException("Không tìm thấy khu vực");
        }

        // Kiểm tra số vé còn lại
        if (areaDetail.getAvailableTickets() < request.getQuantity()) {
            throw new RuntimeException("Không đủ vé trong khu vực");
        }

        // Kiểm tra xem người dùng đã giữ vé ở khu vực khác chưa
        String holdPattern = "ticket:hold:" + userId + ":" + request.getEventId() + ":*";
        Set<String> existingHolds = redisTemplate.keys(holdPattern);
        if (!existingHolds.isEmpty()) {
            for (String holdKey : existingHolds) {
                String[] parts = holdKey.split(":");
                Integer heldAreaId = Integer.parseInt(parts[parts.length - 1]);
                if (!heldAreaId.equals(request.getAreaId())) {
                    throw new RuntimeException("Bạn chỉ được chọn vé từ một khu vực trong một giao dịch");
                }
            }
        }

        // Giữ vé tạm thời trong Redis
        String holdKey = "ticket:hold:" + userId + ":" + request.getEventId() + ":" + request.getAreaId();
        redisTemplate.opsForValue().set(holdKey, String.valueOf(request.getQuantity()), HOLD_TIME_MINUTES, TimeUnit.MINUTES);

        // Giảm tạm thời số vé còn lại trong Redis để tránh xung đột
        String availableKey = "area:available:" + request.getEventId() + ":" + request.getAreaId();
        redisTemplate.opsForValue().setIfAbsent(availableKey, String.valueOf(areaDetail.getAvailableTickets()));
        Long newAvailable = redisTemplate.opsForValue().increment(availableKey, -request.getQuantity());
        if (newAvailable < 0) {
            redisTemplate.opsForValue().increment(availableKey, request.getQuantity()); // Hoàn lại nếu không đủ
            redisTemplate.delete(holdKey);
            throw new RuntimeException("Không đủ vé trong khu vực do người dùng khác đã chọn");
        }

        // Tạo transactionId
        String transactionId = UUID.randomUUID().toString();

        TicketSelectionResponse response = new TicketSelectionResponse();
        response.setTransactionId(transactionId);
        response.setMessage("Chọn vé thành công, vui lòng thanh toán trong " + HOLD_TIME_MINUTES + " phút");
        return response;
    }

    public TicketPurchaseResponse purchaseTickets(TicketPurchaseRequest request, Integer userId, String token) {
        String holdKey = "ticket:hold:" + userId + ":*:" + "*";
        Set<String> holdKeys = redisTemplate.keys(holdKey);
        if (holdKeys.isEmpty()) {
            throw new RuntimeException("Không tìm thấy vé tạm giữ nào để thanh toán");
        }

        String selectedHoldKey = holdKeys.iterator().next();
        String quantityStr = redisTemplate.opsForValue().get(selectedHoldKey);
        if (quantityStr == null) {
            throw new RuntimeException("Phiên tạm giữ vé đã hết hạn");
        }

        String[] parts = selectedHoldKey.split(":");
        Integer eventId = Integer.parseInt(parts[2]);
        Integer areaId = Integer.parseInt(parts[3]);
        Integer quantity = Integer.parseInt(quantityStr);

        //Bổ sung logic thanh toán ở đây (payment service)

        // Giả lập thanh toán thành công, cập nhật trạng thái vé
        String ticketCode = UUID.randomUUID().toString();
        redisTemplate.delete(selectedHoldKey); // Xóa vé tạm giữ

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