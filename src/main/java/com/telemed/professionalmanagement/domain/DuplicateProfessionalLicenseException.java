package com.telemed.professionalmanagement.domain;

public class DuplicateProfessionalLicenseException extends DomainException {

    public DuplicateProfessionalLicenseException(String licenseNumber) {
        super("Professional license number already exists: " + licenseNumber);
    }
}
