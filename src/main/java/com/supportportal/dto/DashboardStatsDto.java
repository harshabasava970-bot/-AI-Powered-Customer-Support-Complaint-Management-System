package com.supportportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {

    // Overall counts
    private long totalComplaints;
    private long newComplaints;
    private long assignedComplaints;
    private long inProgressComplaints;
    private long waitingComplaints;
    private long resolvedComplaints;
    private long closedComplaints;

    // Priority breakdown
    private long criticalComplaints;
    private long highComplaints;
    private long mediumComplaints;
    private long lowComplaints;

    // By category: categoryName → count
    private Map<String, Long> complaintsByCategory;

    // By status: status → count
    private Map<String, Long> complaintsByStatus;

    // Agent workload: agentName → count
    private Map<String, Long> agentWorkload;

    // Satisfaction
    private Double averageRating;
    private long totalFeedbackCount;

    // Total agents
    private long totalAgents;
    private long totalCustomers;
}
