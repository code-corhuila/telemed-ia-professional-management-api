package com.telemed.professionalmanagement.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecialtyRequest(
        @NotBlank(message = "name is required") @Size(max = 100, message = "name must not exceed 100 characters") String name,
        @Size(max = 500, message = "description must not exceed 500 characters") String description
) {
}
