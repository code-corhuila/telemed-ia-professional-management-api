package co.edu.corhuila.telemedia.professionalmanagement.domain.model;

import java.util.UUID;

public class ProfessionalNotFoundException extends DomainException {

    public ProfessionalNotFoundException(UUID professionalId) {
        super("Professional not found with id: " + professionalId);
    }
}
