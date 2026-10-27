package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import java.util.List;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ApiError.FieldDetail;

/** Request values the HTTP adapter rejects before any use case runs ({@code 400}). */
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<FieldDetail> details;

    public InvalidRequestException(List<FieldDetail> details) {
        super("the request is not valid");
        this.details = List.copyOf(details);
    }

    public List<FieldDetail> details() {
        return details;
    }
}
