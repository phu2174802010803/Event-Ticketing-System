package com.example.eventservice.service;

import com.example.eventservice.dto.OrganizerPublicDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.client.RestClientException;

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

    public OrganizerPublicDto getUserPublicInfo(Integer userId) {
        try {
            String url = identityServiceUrl + "/api/users/public/" + userId;
            var response = restTemplate.getForObject(url, UserPublicInfoResponse.class);

            if (response != null) {
                OrganizerPublicDto organizer = new OrganizerPublicDto();
                organizer.setUserId(response.getUserId());
                organizer.setUsername(response.getUsername());
                organizer.setEmail(response.getEmail());
                organizer.setFullName(response.getFullName());
                organizer.setPhoneNumber(response.getPhoneNumber());
                return organizer;
            }
        } catch (RestClientException e) {
            // Log error và trả về thông tin mặc định thay vì throw exception
            System.err.println("Không thể lấy thông tin user từ Identity Service: " + e.getMessage());
        }

        // Trả về thông tin mặc định nếu không lấy được từ Identity Service
        OrganizerPublicDto defaultOrganizer = new OrganizerPublicDto();
        defaultOrganizer.setUserId(userId);
        defaultOrganizer.setUsername("Không có thông tin");
        defaultOrganizer.setEmail("Không có thông tin");
        defaultOrganizer.setFullName("Không có thông tin");
        defaultOrganizer.setPhoneNumber("Không có thông tin");
        return defaultOrganizer;
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

    static class UserPublicInfoResponse {
        private Integer userId;
        private String username;
        private String email;
        private String fullName;
        private String phoneNumber;

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
}
