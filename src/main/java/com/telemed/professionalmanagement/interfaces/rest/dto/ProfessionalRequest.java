package com.telemed.professionalmanagement.interfaces.rest.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfessionalRequest(
        @NotNull(message = "identityUserId is required") UUID identityUserId,
        @NotBlank(message = "licenseNumber is required") @Size(max = 80, message = "licenseNumber must not exceed 80 characters") String licenseNumber,
        @NotNull(message = "yearsExperience is required") @Min(value = 0, message = "yearsExperience must be greater than or equal to 0") Integer yearsExperience
) {
}
