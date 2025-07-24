// src/main/java/com/example/identityservice/service/PaymentClient.java
package com.example.identityservice.service;

import com.example.identityservice.dto.ResponseWrapper;
import com.example.identityservice.dto.UserTransactionHistory.TransactionDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class PaymentClient {
    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseWrapper<List<TransactionDetail>> getTransactionsByUserId(Integer userId, String token) {
        String url = paymentServiceUrl + "/api/admin/transactions/details?userId=" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ResponseWrapper<PageResponse<TransactionDetail>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<ResponseWrapper<PageResponse<TransactionDetail>>>() {
                });

        ResponseWrapper<PageResponse<TransactionDetail>> wrapper = response.getBody();
        if (wrapper != null && wrapper.getData() != null) {
            // Extract the content from Page and wrap it in a new ResponseWrapper
            List<TransactionDetail> transactions = wrapper.getData().getContent();
            return new ResponseWrapper<>(wrapper.getStatus(), wrapper.getMessage(), transactions);
        }

        return new ResponseWrapper<>("error", "No data received", null);
    }

    // Inner class to handle Page response structure
    public static class PageResponse<T> {
        private List<T> content;
        private int totalPages;
        private long totalElements;
        private int size;
        private int number;

        // Getters and setters
        public List<T> getContent() {
            return content;
        }

        public void setContent(List<T> content) {
            this.content = content;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }
}