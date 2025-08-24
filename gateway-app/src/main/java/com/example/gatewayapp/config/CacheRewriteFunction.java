package com.example.gatewayapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
public class CacheRewriteFunction implements RewriteFunction<String, String> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_NAME = "publicEvents";
    private static final int TTL = 300;

    @Override
    public Mono<String> apply(ServerWebExchange exchange, String body) {
        String cacheKey = "cache:" + CACHE_NAME + ":" + exchange.getRequest().getURI().getPath();
        if (body != null) {
            System.out.println("Caching response body for key: " + cacheKey);
            redisTemplate.opsForValue().set(cacheKey, body, TTL, TimeUnit.SECONDS);
        }
        return Mono.just(body);
    }
}