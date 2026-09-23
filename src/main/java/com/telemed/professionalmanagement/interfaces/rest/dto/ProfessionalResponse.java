package com.telemed.professionalmanagement.interfaces.rest.dto;

public record ProfessionalResponse(Long id, Long identityUserId, String licenseNumber, Long specialtyId, Integer yearsExperience) {
}
