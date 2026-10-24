package com.telemed.professionalmanagement.interfaces.rest.dto;

import java.util.UUID;

public record ProfessionalResponse(UUID id, UUID identityUserId, String licenseNumber, Integer yearsExperience,
                                   String professionalType, String status) {
}
