package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code fullName} and {@code email} are left out when the identity domain did not return them (DEC-008); {@code email} also for callers that are not administrators (DEC-009). */
public record ProfessionalResponse(UUID id, UUID identityUserId,
                                   @JsonInclude(JsonInclude.Include.NON_NULL) String fullName,
                                   @JsonInclude(JsonInclude.Include.NON_NULL) String email,
                                   String licenseNumber, Integer yearsExperience,
                                   String professionalType, String status) {
}
