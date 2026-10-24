package com.telemed.professionalmanagement.application.command;

public record RegisterProfessionalCommand(Long identityUserId, String licenseNumber, Integer yearsExperience) {
}
