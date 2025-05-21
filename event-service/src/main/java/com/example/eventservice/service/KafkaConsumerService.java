package com.example.eventservice.service;

import com.example.eventservice.dto.TicketUpdateEvent;
import com.example.eventservice.repository.AreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaConsumerService {

    @Autowired
    private AreaRepository areaRepository;

    @KafkaListener(topics = "ticket-updates", groupId = "event-service")
    @Transactional
    public void handleTicketUpdate(TicketUpdateEvent event) {
        areaRepository.updateAvailableTickets(event.getEventId(), event.getAreaId(), event.getAvailableTickets());
    }
}