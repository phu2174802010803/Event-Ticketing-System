package com.example.gatewayadminorganizer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public AuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if (path.startsWith("/api/organizer/register") ||
                    path.startsWith("/api/auth/login") ||
                    path.startsWith("/api/auth/logout") ||
                    path.startsWith("/api/auth/forgot-password") ||
                    path.startsWith("/api/auth/verify-code") ||
                    path.startsWith("/api/auth/reset-password")) {
                return chain.filter(exchange); // Bỏ qua xác thực
            }

            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // Kiểm tra blacklist trong Redis
                if (redisTemplate.hasKey("blacklist:" + token)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                // Kiểm tra cache Redis
                String userInfo = redisTemplate.opsForValue().get("token:" + token);
                if (userInfo == null) {
                    // Gọi identity-service để validate
                    String validateUrl = "http://localhost:8081/api/auth/validate?token=" + token;
                    String response = restTemplate.getForObject(validateUrl, String.class);
                    if (response == null) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    userInfo = response; // "user_id:role", ví dụ "1:ORGANIZER"
                    // Lưu vào Redis với TTL 10 phút
                    redisTemplate.opsForValue().set("token:" + token, userInfo, 10, TimeUnit.MINUTES);
                }
                // Kiểm tra vai trò
                String[] parts = userInfo.split(":");
                String userId = parts[0];
                String role = parts[1];
                if (!role.equals("ADMIN") && !role.equals("ORGANIZER")) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                // Thêm header X-User-Id và X-User-Role
                exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .build();
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
    }
}