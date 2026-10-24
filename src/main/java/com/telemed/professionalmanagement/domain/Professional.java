package com.telemed.professionalmanagement.domain;

/**
 * A healthcare professional profile. Every professional is a general practitioner (DEC-005)
 * and a new profile starts {@code ACTIVE} (FR-011).
 */
public class Professional {

    private final Long id;
    private final Long identityUserId;
    private final String licenseNumber;
    private final Integer yearsExperience;
    private final ProfessionalType professionalType;
    private final ProfessionalStatus status;

    public Professional(Long id, Long identityUserId, String licenseNumber, Integer yearsExperience,
                        ProfessionalType professionalType, ProfessionalStatus status) {
        validate(identityUserId, licenseNumber, yearsExperience, professionalType, status);
        this.id = id;
        this.identityUserId = identityUserId;
        this.licenseNumber = licenseNumber.trim();
        this.yearsExperience = yearsExperience;
        this.professionalType = professionalType;
        this.status = status;
    }

    /** A profile registered by an administrator: an active general practitioner, not stored yet. */
    public static Professional register(Long identityUserId, String licenseNumber, Integer yearsExperience) {
        return new Professional(null, identityUserId, licenseNumber, yearsExperience,
                ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);
    }

    public Long getId() {
        return id;
    }

    public Long getIdentityUserId() {
        return identityUserId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public ProfessionalType getProfessionalType() {
        return professionalType;
    }

    public ProfessionalStatus getStatus() {
        return status;
    }

    private void validate(Long identityUserId, String licenseNumber, Integer yearsExperience,
                          ProfessionalType professionalType, ProfessionalStatus status) {
        if (identityUserId == null) {
            throw new InvalidBusinessDataException("identityUserId is required");
        }
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new InvalidBusinessDataException("licenseNumber is required");
        }
        if (licenseNumber.trim().length() > 80) {
            throw new InvalidBusinessDataException("licenseNumber must not exceed 80 characters");
        }
        if (yearsExperience == null) {
            throw new InvalidBusinessDataException("yearsExperience is required");
        }
        if (yearsExperience < 0) {
            throw new InvalidBusinessDataException("yearsExperience must be greater than or equal to 0");
        }
        if (professionalType == null) {
            throw new InvalidBusinessDataException("professionalType is required");
        }
        if (status == null) {
            throw new InvalidBusinessDataException("status is required");
        }
    }
}
