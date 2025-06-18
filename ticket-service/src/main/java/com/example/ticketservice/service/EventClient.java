package com.example.ticketservice.service;

import com.example.ticketservice.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;

@Component
public class EventClient {

    @Value("${event.service.url}")
    private String eventServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public EventPublicDetailDto getPublicEventDetail(Integer eventId, String token) {
        String url = eventServiceUrl + "/api/events/public/" + eventId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<EventPublicDetailDto> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                EventPublicDetailDto.class);
        return response.getBody();
    }

    public AreaDetailDto getAreaDetail(Integer eventId, Integer areaId, String token) {
        String url = eventServiceUrl + "/api/events/" + eventId + "/areas/" + areaId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<AreaDetailDto> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                AreaDetailDto.class);
        return response.getBody();
    }

    public SellingPhaseResponse[] getSellingPhases(Integer eventId, String token) {
        String url = eventServiceUrl + "/api/events/public/" + eventId + "/phases";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<SellingPhaseResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                SellingPhaseResponse[].class);
        return response.getBody();
    }

    public EventInfo getEventInfo(Integer eventId, String token) {
        String url = eventServiceUrl + "/api/events/public/" + eventId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<EventInfo> response = restTemplate.exchange(url, HttpMethod.GET, entity, EventInfo.class);
        return response.getBody();
    }

    // Method for Admins to fetch areas
    public List<AreaDetailDto> getAreasByEvent(Integer eventId, String token) {
        String url = eventServiceUrl + "/api/admin/events/" + eventId + "/areas";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        AreaDetailDto[] areas = restTemplate.exchange(url, HttpMethod.GET, entity, AreaDetailDto[].class).getBody();
        return Arrays.asList(areas != null ? areas : new AreaDetailDto[0]);
    }

    // New method for Organizers to fetch areas
    public List<AreaResponseDto> getAreasByEventForOrganizer(Integer eventId, String token) {
        String url = eventServiceUrl + "/api/organizer/events/" + eventId + "/areas";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ResponseWrapper<List<AreaResponseDto>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ResponseWrapper<List<AreaResponseDto>>>() {
                });
        ResponseWrapper<List<AreaResponseDto>> wrapper = response.getBody();
        return wrapper != null && wrapper.getData() != null ? wrapper.getData() : Arrays.asList(new AreaResponseDto[0]);
    }

    public List<AreaResponseDto> getAreasByEventForAdmin(Integer eventId, String token) {
        String url = eventServiceUrl + "/api/admin/events/" + eventId + "/areas";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ResponseWrapper<List<AreaResponseDto>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ResponseWrapper<List<AreaResponseDto>>>() {
                });
        ResponseWrapper<List<AreaResponseDto>> wrapper = response.getBody();
        return wrapper != null && wrapper.getData() != null ? wrapper.getData() : Arrays.asList(new AreaResponseDto[0]);
    }

    public List<EventInfo> getAllEvents(String token) {
        String url = eventServiceUrl + "/api/admin/events";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        EventInfo[] events = restTemplate.exchange(url, HttpMethod.GET, entity, EventInfo[].class).getBody();
        return Arrays.asList(events != null ? events : new EventInfo[0]);
    }

    public boolean checkEventOwnership(Integer organizerId, Integer eventId, String token) {
        String url = eventServiceUrl + "/api/organizer/events/" + eventId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, Void.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Integer> getOrganizerEventIds(Integer organizerId, String token) {
        String url = eventServiceUrl + "/api/organizer/events";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // The /api/organizer/events endpoint returns List<EventDetailResponseDto>
            // directly, not wrapped
            ResponseEntity<LinkedHashMap[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    LinkedHashMap[].class);

            LinkedHashMap[] events = response.getBody();
            if (events != null) {
                return Arrays.stream(events)
                        .map(event -> (Integer) event.get("eventId"))
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("Error fetching organizer events: " + e.getMessage());
            e.printStackTrace();
        }
        return Arrays.asList();
    }
}