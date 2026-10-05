package com.supportportal.dto;

import com.supportportal.entity.ComplaintPriority;
import com.supportportal.entity.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintSearchRequest {

    private String keyword;        // search in title
    private Long complaintId;
    private String customerName;
    private Long categoryId;
    private ComplaintPriority priority;
    private ComplaintStatus status;
    private Long agentId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
