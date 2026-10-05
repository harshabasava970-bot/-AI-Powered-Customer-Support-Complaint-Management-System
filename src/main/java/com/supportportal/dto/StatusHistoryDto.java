package com.supportportal.dto;

import com.supportportal.entity.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistoryDto {

    private Long id;
    private ComplaintStatus oldStatus;
    private ComplaintStatus newStatus;
    private String changedByName;
    private String notes;
    private LocalDateTime changedAt;
}
