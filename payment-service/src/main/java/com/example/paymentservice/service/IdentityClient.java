package com.example.paymentservice.service;

import com.example.paymentservice.dto.UserPublicDto;
import com.example.paymentservice.dto.UserResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
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

    public UserPublicDto getUserPublicInfo(Integer userId) {
        try {
            String url = identityServiceUrl + "/api/users/public/" + userId;
            var response = restTemplate.getForObject(url, UserPublicDto.class);
            return response;
        } catch (RestClientException e) {
            // Log error và trả về thông tin mặc định thay vì throw exception
            System.err.println("Không thể lấy thông tin user từ Identity Service: " + e.getMessage());

            // Trả về thông tin mặc định nếu không lấy được từ Identity Service
            UserPublicDto defaultUser = new UserPublicDto();
            defaultUser.setUserId(userId);
            defaultUser.setUsername("Không có thông tin");
            defaultUser.setEmail("Không có thông tin");
            defaultUser.setFullName("Không có thông tin");
            defaultUser.setPhoneNumber("Không có thông tin");
            return defaultUser;
        }
    }
}