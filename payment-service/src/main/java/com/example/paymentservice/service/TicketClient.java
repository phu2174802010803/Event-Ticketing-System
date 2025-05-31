// src/main/java/com/example/paymentservice/service/TicketClient.java
package com.example.paymentservice.service;

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
public class TicketClient {
    @Value("${ticket.service.url}")
    private String ticketServiceUrl;

    private final RestTemplate restTemplate;

    public TicketClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Object> getTicketsByTransactionId(String transactionId, String token) {
        String url = ticketServiceUrl + "/api/admin/tickets/by-transaction?transactionId=" + transactionId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Object>>() {
                    });
            return response.getBody();
        } catch (Exception e) {
            // Log error and return empty list if ticket service is unavailable
            System.err.println("Error calling ticket service: " + e.getMessage());
            return List.of(); // Return empty list instead of null
        }
    }
}