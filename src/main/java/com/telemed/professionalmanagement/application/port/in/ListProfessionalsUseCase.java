package com.telemed.professionalmanagement.application.port.in;

public interface ListProfessionalsUseCase {

    /** Any authenticated caller reads the professionals, one bounded page at a time. */
    Page<ProfessionalView> list(PageRequest page);
}
