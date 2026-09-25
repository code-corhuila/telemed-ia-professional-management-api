package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.domain.Specialty;
import java.util.List;

public interface ListSpecialtiesUseCase {
    List<Specialty> listAll();
}
