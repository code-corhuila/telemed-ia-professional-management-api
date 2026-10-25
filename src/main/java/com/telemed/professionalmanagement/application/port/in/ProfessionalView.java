package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.application.port.out.AccountName;
import com.telemed.professionalmanagement.domain.Professional;

/**
 * A professional as it is read: the profile and, when the identity domain returned it, the
 * name of its account ({@code null} otherwise, DEC-008).
 */
public record ProfessionalView(Professional professional, AccountName account) {
}
