package com.telemed.professionalmanagement.domain;

public class Professional {

    private final Long id;
    private final Long identityUserId;
    private final String licenseNumber;
    private final Long specialtyId;
    private final Integer yearsExperience;

    public Professional(Long id, Long identityUserId, String licenseNumber, Long specialtyId, Integer yearsExperience) {
        validate(identityUserId, licenseNumber, specialtyId, yearsExperience);
        this.id = id;
        this.identityUserId = identityUserId;
        this.licenseNumber = licenseNumber.trim();
        this.specialtyId = specialtyId;
        this.yearsExperience = yearsExperience;
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

    public Long getSpecialtyId() {
        return specialtyId;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    private void validate(Long identityUserId, String licenseNumber, Long specialtyId, Integer yearsExperience) {
        if (identityUserId == null) {
            throw new InvalidBusinessDataException("identityUserId is required");
        }
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new InvalidBusinessDataException("licenseNumber is required");
        }
        if (licenseNumber.trim().length() > 80) {
            throw new InvalidBusinessDataException("licenseNumber must not exceed 80 characters");
        }
        if (specialtyId == null) {
            throw new InvalidBusinessDataException("specialtyId is required");
        }
        if (yearsExperience == null) {
            throw new InvalidBusinessDataException("yearsExperience is required");
        }
        if (yearsExperience < 0) {
            throw new InvalidBusinessDataException("yearsExperience must be greater than or equal to 0");
        }
    }
}
