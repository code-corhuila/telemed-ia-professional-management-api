package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

/** The caller is authenticated but may not run the operation ({@code 403 FORBIDDEN}). */
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException() {
        super("the operation is not allowed for this user");
    }
}
