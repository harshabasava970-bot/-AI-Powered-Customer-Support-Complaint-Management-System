package com.supportportal.dto;

import com.supportportal.entity.ComplaintPriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplaintRequest {

    @NotNull(message = "Priority is required")
    private ComplaintPriority priority;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Size(max = 500)
    private String additionalInfo;
}
