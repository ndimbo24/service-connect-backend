package com.serviceconnect.service;

import com.serviceconnect.dto.response.ActivityLogResponse;
import com.serviceconnect.entity.ActivityLog;
import com.serviceconnect.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void log(Long userId, String userName, String userRole, String action, String details) {
        ActivityLog log = ActivityLog.builder()
                .userId(userId)
                .userName(userName)
                .userRole(userRole)
                .action(action)
                .details(details)
                .build();
        activityLogRepository.save(log);
    }

    public Page<ActivityLogResponse> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toResponse);
    }

    // Keep the old method for backward compatibility
    public List<ActivityLogResponse> getAllLogs() {
        return getAllLogs(Pageable.unpaged()).getContent();
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userName(log.getUserName())
                .userRole(log.getUserRole())
                .action(log.getAction())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}
