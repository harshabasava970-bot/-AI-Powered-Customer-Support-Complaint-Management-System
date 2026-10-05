package com.supportportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveComplaintRequest {

    @NotBlank(message = "Resolution notes are required")
    @Size(min = 10, message = "Resolution notes must be at least 10 characters")
    private String resolutionNotes;
}
