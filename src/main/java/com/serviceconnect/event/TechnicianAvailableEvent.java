package com.serviceconnect.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired whenever a technician becomes available for matching.
 * This decouples availability changes from the matching logic,
 * avoiding circular dependencies between services.
 */
@Getter
public class TechnicianAvailableEvent extends ApplicationEvent {

    private final Long technicianId;

    public TechnicianAvailableEvent(Object source, Long technicianId) {
        super(source);
        this.technicianId = technicianId;
    }
}

