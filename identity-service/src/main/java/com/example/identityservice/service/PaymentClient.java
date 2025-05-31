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
        String url = paymentServiceUrl + "/api/admin/transactions?userId=" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ResponseWrapper<List<TransactionDetail>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<ResponseWrapper<List<TransactionDetail>>>() {}
        );
        return response.getBody();
    }
}