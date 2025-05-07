package com.example.ticketservice.service;

import com.example.ticketservice.dto.AreaDetailDto;
import com.example.ticketservice.dto.SellingPhaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EventClient {

    @Value("${event.service.url}")
    private String eventServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public AreaDetailDto getAreaDetail(Integer eventId, Integer areaId, String token) {
        String url = eventServiceUrl + "/api/events/" + eventId + "/areas/" + areaId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<AreaDetailDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, AreaDetailDto.class);
        return response.getBody();
    }

    public SellingPhaseResponse[] getSellingPhases(Integer eventId, String token) {
        String url = eventServiceUrl + "/api/events/public/" + eventId + "/phases";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<SellingPhaseResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, SellingPhaseResponse[].class);
        return response.getBody();
    }
}