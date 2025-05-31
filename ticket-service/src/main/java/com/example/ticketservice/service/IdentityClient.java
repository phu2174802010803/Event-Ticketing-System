package com.example.ticketservice.service;

import com.example.ticketservice.dto.UserResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class IdentityClient {

    @Value("${identity.service.url}")
    private String identityServiceUrl;

    private final RestTemplate restTemplate;

    public IdentityClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserResponseDto getUserDetail(Integer userId, String token) {
        String url = identityServiceUrl + "/api/users/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<UserResponseDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, UserResponseDto.class);
        return response.getBody();
    }
}