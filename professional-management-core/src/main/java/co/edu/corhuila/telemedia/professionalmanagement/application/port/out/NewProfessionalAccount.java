package co.edu.corhuila.telemedia.professionalmanagement.application.port.out;

/** The account the identity domain creates for a professional (role {@code PROFESSIONAL}). */
public record NewProfessionalAccount(String fullName, String email, String identityDocument, String password) {

    /** Never prints the password. */
    @Override
    public String toString() {
        return "NewProfessionalAccount[fullName=" + fullName + ", email=" + email
                + ", identityDocument=" + identityDocument + ", password=***]";
    }
}
