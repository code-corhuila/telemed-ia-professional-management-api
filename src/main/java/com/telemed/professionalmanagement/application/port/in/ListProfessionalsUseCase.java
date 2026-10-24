package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.domain.Professional;

public interface ListProfessionalsUseCase {

    /** Any authenticated caller reads the professionals, one bounded page at a time. */
    Page<Professional> list(PageRequest page);
}
