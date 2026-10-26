package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /admin/register} (professional-service.yaml); every field is required. */
public record RegisterProfessionalRequest(
        @NotBlank(message = "is required") String fullName,
        @NotBlank(message = "is required") @Email(message = "must be an e-mail address") String email,
        @NotBlank(message = "is required") String identityDocument,
        @NotNull(message = "is required") @Size(min = 8, message = "must have at least 8 characters") String password,
        @NotBlank(message = "is required") @Size(max = 80, message = "must not exceed 80 characters") String licenseNumber,
        @NotNull(message = "is required") @Min(value = 0, message = "must be 0 or greater") Integer yearsExperience) {

    /** Never prints the password. */
    @Override
    public String toString() {
        return "RegisterProfessionalRequest[fullName=" + fullName + ", email=" + email + ", identityDocument="
                + identityDocument + ", password=***, licenseNumber=" + licenseNumber + ", yearsExperience="
                + yearsExperience + "]";
    }
}
