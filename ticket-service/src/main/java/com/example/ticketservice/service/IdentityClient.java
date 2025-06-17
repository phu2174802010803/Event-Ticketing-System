package com.example.ticketservice.service;

import com.example.ticketservice.dto.UserResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        try {
            // Thử gọi endpoint có authentication trước
            String url = identityServiceUrl + "/api/users/" + userId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<UserResponseDto> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    UserResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            // Nếu thất bại, thử endpoint public
            try {
                String publicUrl = identityServiceUrl + "/api/users/public/" + userId;
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(publicUrl, JsonNode.class);

                // Map JsonNode to UserResponseDto
                UserResponseDto userDto = new UserResponseDto();
                JsonNode node = response.getBody();
                if (node != null) {
                    userDto.setUserId(node.get("userId").asInt());
                    userDto.setUsername(node.get("username").asText());
                    userDto.setEmail(node.get("email").asText());
                    userDto.setFullName(node.get("fullName").asText());
                    userDto.setPhoneNumber(node.get("phoneNumber") != null ? node.get("phoneNumber").asText() : null);
                    // Set default values for missing fields
                    userDto.setRole("USER");
                    userDto.setIsActive(true);
                    userDto.setCreatedAt(null);
                }

                return userDto;
            } catch (Exception publicException) {
                throw new RuntimeException("Failed to get user details from both authenticated and public endpoints",
                        publicException);
            }
        }
    }
}