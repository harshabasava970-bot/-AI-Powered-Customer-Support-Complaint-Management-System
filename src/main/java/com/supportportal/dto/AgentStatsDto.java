package com.supportportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentStatsDto {

    private Long agentId;
    private String agentName;
    private String agentEmail;

    private long totalAssigned;
    private long inProgress;
    private long resolved;
    private long closed;
    private long waitingForCustomer;

    private Double averageRating;
    private long feedbackCount;
}
