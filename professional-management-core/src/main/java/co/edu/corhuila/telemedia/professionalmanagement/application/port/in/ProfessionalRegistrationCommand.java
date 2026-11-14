package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

/**
 * {@code POST /admin/register}: the account of the professional in the identity domain and
 * the profile of this domain, registered together (DEC-003).
 */
public record ProfessionalRegistrationCommand(String fullName, String email, String identityDocument,
                                              String password, String licenseNumber, Integer yearsExperience) {

    /** Never prints the password, so the command is safe in logs and test reports. */
    @Override
    public String toString() {
        return "ProfessionalRegistrationCommand[fullName=" + fullName + ", email=" + email
                + ", identityDocument=" + identityDocument + ", password=***, licenseNumber=" + licenseNumber
                + ", yearsExperience=" + yearsExperience + "]";
    }
}
