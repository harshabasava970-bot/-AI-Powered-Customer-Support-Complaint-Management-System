package com.supportportal.dto;

import com.supportportal.entity.ComplaintPriority;
import com.supportportal.entity.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintDto {

    private Long id;
    private String title;
    private String description;
    private ComplaintStatus status;
    private ComplaintPriority priority;

    // Category
    private Long categoryId;
    private String categoryName;

    // Customer info
    private Long customerId;
    private String customerName;
    private String customerEmail;

    // Assigned agent info
    private Long assignedAgentId;
    private String assignedAgentName;

    // AI/NLP fields
    private String aiPredictedCategory;
    private ComplaintPriority aiPredictedPriority;
    private String aiAnalysisExplanation;
    private Double aiConfidenceScore;

    // Resolution
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private String additionalInfo;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Feedback
    private ComplaintFeedbackDto feedback;

    // Status history
    private List<StatusHistoryDto> statusHistory;
}
