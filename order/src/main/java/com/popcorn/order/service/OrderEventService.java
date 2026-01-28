package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.popcorn.order.dto.event.OrderEventResponse;
import com.popcorn.order.dto.event.OrderEventSummary;


public interface OrderEventService {

   
    List<OrderEventResponse> getOrderEventHistory(UUID orderId);


    Page<OrderEventSummary> getSystemEvents(
            String eventType,
            Long userId,
            LocalDateTime from,
            LocalDateTime to,
            String status,
            Pageable pageable
    );

    String replayOrderEvents(UUID orderId, LocalDateTime upTo);

    Object getEventMetrics(LocalDateTime from, LocalDateTime to, String aggregateBy);

    String saveEvent(UUID orderId, String eventType, Object eventData, Long userId);

   
    void updateEventStatus(String eventId, String status, Long processingTimeMs, String errorMessage);

   
    int retryFailedEvents(int maxRetryCount);

   
    int cleanupOldEvents(int olderThanDays);

}
