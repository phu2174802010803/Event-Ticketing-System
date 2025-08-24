package com.example.ticketservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class QueueService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final int MAX_ACTIVE_USERS = 1; // Giới hạn 2 người dùng đồng thời
    private static final int MAX_QUEUE_SIZE = 1;   // Hàng đợi 1000 người
    private static final int ACCESS_TIME_MINUTES = 2; // TTL cho người dùng hoạt động
    private static final int CONFIRMATION_INTERVAL_MINUTES = 1; // TTL cho người dùng trong hàng đợi

    // Kiểm tra khả năng tham gia hàng đợi
    public boolean canJoinQueue(Integer eventId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        Long activeCount = redisTemplate.opsForZSet().size(activeKey);
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        return (activeCount != null && activeCount < MAX_ACTIVE_USERS) ||
                (queueSize != null && queueSize < MAX_QUEUE_SIZE);
    }

    // Tham gia hàng đợi
    public String joinQueue(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        String userKey = "user:" + eventId + ":" + userId;
        String userActiveKey = "user_active:" + eventId + ":" + userId;

        Double score = redisTemplate.opsForZSet().score(activeKey, userId.toString());
        if (score != null) {
            Long rank = redisTemplate.opsForZSet().rank(activeKey, userId.toString());
            return "Bạn đã ở trong danh sách hoạt động, vị trí thứ " + (rank != null ? rank + 1 : "không xác định");
        }

        Long activeCount = redisTemplate.opsForZSet().size(activeKey);
        if (activeCount != null && activeCount < MAX_ACTIVE_USERS) {
            long timestamp = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(activeKey, userId.toString(), timestamp);
            redisTemplate.opsForValue().set(userActiveKey, "active", ACCESS_TIME_MINUTES, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(userKey, "active");
            // Gửi thông báo qua WebSocket
            messagingTemplate.convertAndSend("/topic/queue/" + eventId,
                    "{\"userId\": " + userId + ", \"message\": \"Bạn có thể truy cập ngay bây giờ\"}");
            return "Bạn có thể truy cập ngay lập tức";
        } else {
            Long queueSize = redisTemplate.opsForList().size(queueKey);
            if (queueSize != null && queueSize >= MAX_QUEUE_SIZE) {
                return "Hàng đợi đã đầy, vui lòng chờ thông báo";
            }
            redisTemplate.opsForList().rightPush(queueKey, userId.toString());
            redisTemplate.opsForValue().set(userKey, "queue", CONFIRMATION_INTERVAL_MINUTES, TimeUnit.MINUTES);
            Long position = redisTemplate.opsForList().size(queueKey);
            return "Bạn đang ở vị trí thứ " + position + " trong hàng đợi";
        }
    }

    // Kiểm tra trạng thái hàng đợi với caching
    @Cacheable(value = "queueStatus", key = "#eventId + ':' + #userId")
    public String checkQueueStatus(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        String userKey = "user:" + eventId + ":" + userId;

        String status = redisTemplate.opsForValue().get(userKey);
        if ("active".equals(status)) {
            Long rank = redisTemplate.opsForZSet().rank(activeKey, userId.toString());
            return "Bạn đang ở vị trí thứ " + (rank != null ? rank + 1 : "không xác định") + " trong danh sách hoạt động";
        } else if ("queue".equals(status)) {
            Long position = redisTemplate.opsForList().indexOf(queueKey, userId.toString());
            if (position != null && position >= 0) {
                return "Bạn đang ở vị trí thứ " + (position + 1) + " trong hàng đợi";
            }
        }
        return "Bạn không ở trong hàng đợi";
    }

    // Xác nhận tiếp tục trong hàng đợi
    public void confirmPresence(Integer eventId, Integer userId) {
        String userKey = "user:" + eventId + ":" + userId;
        String status = redisTemplate.opsForValue().get(userKey);
        if ("queue".equals(status)) {
            redisTemplate.expire(userKey, CONFIRMATION_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }
    }

    // Rời hàng đợi
    public void leaveQueue(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        String userKey = "user:" + eventId + ":" + userId;
        String userActiveKey = "user_active:" + eventId + ":" + userId;

        redisTemplate.opsForZSet().remove(activeKey, userId.toString());
        redisTemplate.opsForList().remove(queueKey, 0, userId.toString());
        redisTemplate.delete(userKey);
        redisTemplate.delete(userActiveKey);
    }

    // Xử lý khi người dùng hết hạn (được gọi từ Redis Pub/Sub)
    public void handleUserExpired(String message) {
        if (message.startsWith("user_active:")) {
            String[] parts = message.split(":");
            Integer eventId = Integer.parseInt(parts[1]);
            Integer userId = Integer.parseInt(parts[2]);
            removeUserFromActive(eventId, userId);
            moveFromQueueToActive(eventId);
        }
    }

    // Xóa người dùng khỏi danh sách hoạt động
    private void removeUserFromActive(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        String userKey = "user:" + eventId + ":" + userId;
        redisTemplate.opsForZSet().remove(activeKey, userId.toString());
        redisTemplate.delete(userKey);
    }

    // Chuyển người dùng từ hàng đợi lên danh sách hoạt động
    private void moveFromQueueToActive(Integer eventId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        Long activeCount = redisTemplate.opsForZSet().size(activeKey);

        while (activeCount != null && activeCount < MAX_ACTIVE_USERS && redisTemplate.opsForList().size(queueKey) > 0) {
            String nextUser = redisTemplate.opsForList().leftPop(queueKey);
            if (nextUser != null) {
                Integer userId = Integer.parseInt(nextUser);
                String userKey = "user:" + eventId + ":" + userId;
                String userActiveKey = "user_active:" + eventId + ":" + userId;
                long timestamp = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(activeKey, nextUser, timestamp);
                redisTemplate.opsForValue().set(userKey, "active");
                redisTemplate.opsForValue().set(userActiveKey, "active", ACCESS_TIME_MINUTES, TimeUnit.MINUTES);
                // Gửi thông báo qua WebSocket
                messagingTemplate.convertAndSend("/topic/queue/" + eventId,
                        "{\"userId\": " + userId + ", \"message\": \"Bạn đã được chuyển sang danh sách hoạt động\"}");
                activeCount = redisTemplate.opsForZSet().size(activeKey);
            }
        }
    }
}