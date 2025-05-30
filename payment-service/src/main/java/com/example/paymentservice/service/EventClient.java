package com.example.paymentservice.service;

import com.example.paymentservice.dto.EventDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventClient {
    @Value("${event.service.url}")
    private String eventServiceUrl;

    private final RestTemplate restTemplate;

    public EventClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Integer> getOrganizerEventIds(Integer organizerId, String token) {
        String url = eventServiceUrl + "/api/organizer/events?organizerId=" + organizerId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<EventDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<EventDto>>() {}
        );

        List<EventDto> events = response.getBody();
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        return events.stream()
                .map(EventDto::getEventId)
                .collect(Collectors.toList());
    }
}