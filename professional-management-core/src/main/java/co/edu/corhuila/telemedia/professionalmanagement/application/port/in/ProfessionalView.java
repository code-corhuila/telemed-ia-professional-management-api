package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;

/**
 * A professional as it is read: the profile and, when the identity domain returned it, the
 * name of its account ({@code null} otherwise, DEC-008).
 */
public record ProfessionalView(Professional professional, AccountName account) {
}
