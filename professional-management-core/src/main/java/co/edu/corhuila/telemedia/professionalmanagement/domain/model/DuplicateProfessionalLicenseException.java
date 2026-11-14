package co.edu.corhuila.telemedia.professionalmanagement.domain.model;

public class DuplicateProfessionalLicenseException extends DomainException {

    public DuplicateProfessionalLicenseException(String licenseNumber) {
        super("Professional license number already exists: " + licenseNumber);
    }
}
