package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.popcorn.order.dto.audit.ApiLogEntry;
import com.popcorn.order.dto.audit.BusinessEventLog;


public interface AuditLogService {

   
    void logApiRequest(ApiLogEntry logEntry);

    void logApiCompletion(String requestId, Integer responseStatus, Long processingTime, Exception exception);


    void logBusinessEvent(BusinessEventLog eventLog);

  
    void logSecurityEvent(String eventType, Long userId, String clientIp, String description, Map<String, Object> metadata);


    void logSystemEvent(String eventType, String severity, String description, Map<String, Object> metadata);

   
    Page<ApiLogEntry> searchApiLogs(LocalDateTime startTime, LocalDateTime endTime,
                                   Long userId, String method, Integer status, Pageable pageable);

    Page<BusinessEventLog> searchBusinessEventLogs(LocalDateTime startTime, LocalDateTime endTime,
                                                   String eventType, Long userId, Pageable pageable);


    Page<Object> searchSecurityEventLogs(LocalDateTime startTime, LocalDateTime endTime,
                                        String eventType, Long userId, String clientIp, Pageable pageable);


    Map<String, Object> getLogStatistics(LocalDateTime startTime, LocalDateTime endTime, String logType);

   
    Map<String, Object> generatePerformanceReport(LocalDateTime startTime, LocalDateTime endTime);


    List<Map<String, Object>> detectSuspiciousActivities(int lookbackHours);

   
    long cleanupOldLogs(int retentionDays, String logType);

 
    long archiveLogs(int archiveBeforeDays, String logType);

  
    List<Map<String, Object>> getEventsRequiringNotification(int minutes);

}
