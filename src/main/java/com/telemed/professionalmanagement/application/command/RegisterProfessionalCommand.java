package com.telemed.professionalmanagement.application.command;

import java.util.UUID;

public record RegisterProfessionalCommand(UUID identityUserId, String licenseNumber, Integer yearsExperience) {
}
