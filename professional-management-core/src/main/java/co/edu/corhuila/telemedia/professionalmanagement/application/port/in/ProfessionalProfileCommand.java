package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

import java.util.UUID;

/** The profile of an account that Identity & Access already created (DEC-015). */
public record ProfessionalProfileCommand(UUID accountId, String licenseNumber, Integer yearsExperience) {
}
