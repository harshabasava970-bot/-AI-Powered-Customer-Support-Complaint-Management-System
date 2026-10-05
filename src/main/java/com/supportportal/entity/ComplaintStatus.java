package com.supportportal.entity;

/**
 * Lifecycle: NEW → ASSIGNED → IN_PROGRESS → WAITING_FOR_CUSTOMER → RESOLVED → CLOSED
 */
public enum ComplaintStatus {
    NEW,
    ASSIGNED,
    IN_PROGRESS,
    WAITING_FOR_CUSTOMER,
    RESOLVED,
    CLOSED
}
