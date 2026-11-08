package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Body of {@code POST /internal/professionals} (DEC-015); every field is required. */
public record CreateProfileRequest(
        @NotNull(message = "is required") UUID accountId,
        @NotBlank(message = "is required") @Size(max = 80, message = "must not exceed 80 characters") String licenseNumber,
        @NotNull(message = "is required") @Min(value = 0, message = "must be 0 or greater") Integer yearsExperience) {
}
