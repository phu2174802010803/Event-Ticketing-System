package com.example.ticketservice.service;

import com.example.ticketservice.dto.ResponseWrapper;
import com.example.ticketservice.dto.TransactionResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public TransactionResponseDto getTransactionDetail(String transactionId, String token) {
        String url = paymentServiceUrl + "/api/payments/transactions/" + transactionId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Sử dụng ParameterizedTypeReference để xử lý
        // ResponseWrapper<TransactionResponseDto>
        ParameterizedTypeReference<ResponseWrapper<TransactionResponseDto>> responseType = new ParameterizedTypeReference<ResponseWrapper<TransactionResponseDto>>() {
        };

        ResponseEntity<ResponseWrapper<TransactionResponseDto>> response = restTemplate.exchange(url, HttpMethod.GET,
                entity, responseType);

        // Trả về data từ ResponseWrapper
        return response.getBody() != null ? response.getBody().getData() : null;
    }
}