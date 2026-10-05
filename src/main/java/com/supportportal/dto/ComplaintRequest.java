package com.supportportal.dto;

import com.supportportal.entity.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be 5–200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private ComplaintPriority priority;

    private String additionalInfo;
}
