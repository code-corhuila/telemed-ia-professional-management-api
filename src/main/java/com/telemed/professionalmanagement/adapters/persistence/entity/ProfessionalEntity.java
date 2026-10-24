package com.telemed.professionalmanagement.adapters.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "professionals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_professionals_identity_user_id", columnNames = "identity_user_id"),
                @UniqueConstraint(name = "uk_professionals_license_number", columnNames = "license_number")
        }
)
public class ProfessionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_user_id", nullable = false, unique = true)
    private Long identityUserId;

    @Column(name = "license_number", nullable = false, unique = true, length = 80)
    private String licenseNumber;

    @Column(name = "years_experience", nullable = false)
    private Integer yearsExperience;

    @Column(name = "professional_type", nullable = false, length = 40)
    private String professionalType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    protected ProfessionalEntity() {
    }

    public ProfessionalEntity(Long id, Long identityUserId, String licenseNumber, Integer yearsExperience,
                              String professionalType, String status) {
        this.id = id;
        this.identityUserId = identityUserId;
        this.licenseNumber = licenseNumber;
        this.yearsExperience = yearsExperience;
        this.professionalType = professionalType;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdentityUserId() {
        return identityUserId;
    }

    public void setIdentityUserId(Long identityUserId) {
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
}
