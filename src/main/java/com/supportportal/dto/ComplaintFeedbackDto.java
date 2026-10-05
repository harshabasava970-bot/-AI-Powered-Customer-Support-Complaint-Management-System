package com.supportportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintFeedbackDto {

    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime submittedAt;
    private Long complaintId;
}
