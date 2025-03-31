package com.example.eventservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;



@Component
public class IdentityClient {

    @Value("${identity.service.url}")
    private String identityServiceUrl;

    private final RestTemplate restTemplate;

    public IdentityClient() {
        this.restTemplate = new RestTemplate();
    }

    public Integer getUserId(String token) {
        String url = identityServiceUrl + "/api/users/profile";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        var response = restTemplate.exchange(url, HttpMethod.GET, entity, UserProfileResponse.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getUserId();
        }
        throw new RuntimeException("Không thể lấy userId từ Identity Service");
    }

    static class UserProfileResponse {
        private Integer userId;

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }
    }
}
