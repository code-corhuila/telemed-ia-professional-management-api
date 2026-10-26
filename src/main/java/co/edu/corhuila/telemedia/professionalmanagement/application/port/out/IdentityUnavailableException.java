package co.edu.corhuila.telemedia.professionalmanagement.application.port.out;

/** The identity domain could not create the account now; nothing was registered. */
public class IdentityUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdentityUnavailableException(String reason) {
        super(reason);
    }
}
