package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

public interface ListProfessionalsUseCase {

    /**
     * Any authenticated caller reads the professionals, one bounded page at a time; the account
     * e-mail only when the caller is an administrator (DEC-009).
     */
    Page<ProfessionalView> list(Caller caller, PageRequest page);
}
