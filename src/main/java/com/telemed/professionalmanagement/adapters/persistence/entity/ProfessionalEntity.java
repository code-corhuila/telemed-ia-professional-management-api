package com.telemed.professionalmanagement.adapters.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        schema = "professional_management",
        name = "professional",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_professionals_identity_user_id", columnNames = "identity_user_id"),
                @UniqueConstraint(name = "uq_professionals_license_number", columnNames = "license_number")
        }
)
public class ProfessionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "identity_user_id", nullable = false, unique = true)
    private UUID identityUserId;

    @Column(name = "license_number", nullable = false, unique = true, length = 80)
    private String licenseNumber;

    @Column(name = "years_experience", nullable = false)
    private Integer yearsExperience;

    @Column(name = "professional_type", nullable = false, length = 40)
    private String professionalType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Set by the database when the profile is inserted (DEC-010); only read to order the list. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ProfessionalEntity() {
    }

    public ProfessionalEntity(UUID id, UUID identityUserId, String licenseNumber, Integer yearsExperience,
                              String professionalType, String status) {
        this.id = id;
        this.identityUserId = identityUserId;
        this.licenseNumber = licenseNumber;
        this.yearsExperience = yearsExperience;
        this.professionalType = professionalType;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdentityUserId() {
        return identityUserId;
    }

    public void setIdentityUserId(UUID identityUserId) {
        this.identityUserId = identityUserId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public String getProfessionalType() {
        return professionalType;
    }

    public void setProfessionalType(String professionalType) {
        this.professionalType = professionalType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
