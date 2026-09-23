package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.domain.Professional;
import java.util.List;

public interface ListProfessionalsUseCase {
    List<Professional> listAll();
}
