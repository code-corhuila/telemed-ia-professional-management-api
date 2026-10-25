package com.telemed.professionalmanagement.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.application.port.in.ProfessionalView;
import com.telemed.professionalmanagement.application.port.out.AccountDirectory;
import com.telemed.professionalmanagement.application.port.out.AccountName;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;

/**
 * Reads the professionals with the names of their accounts (DEC-008): one identity call per
 * page, and the professionals are returned without names when identity cannot answer.
 */
@Service
public class ProfessionalApplicationService implements ListProfessionalsUseCase, GetProfessionalUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ProfessionalApplicationService.class);

    private final ProfessionalRepositoryPort professionalRepositoryPort;
    private final AccountDirectory accountDirectory;

    public ProfessionalApplicationService(ProfessionalRepositoryPort professionalRepositoryPort,
                                          AccountDirectory accountDirectory) {
        this.professionalRepositoryPort = professionalRepositoryPort;
        this.accountDirectory = accountDirectory;
    }

    @Override
    public Page<ProfessionalView> list(PageRequest page) {
        Page<Professional> stored = professionalRepositoryPort.findPage(page);
        return new Page<>(withNames(stored.items()), stored.page(), stored.limit(), stored.total());
    }

    @Override
    public ProfessionalView getById(UUID professionalId) {
        Professional professional = professionalRepositoryPort.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
        return withNames(List.of(professional)).get(0);
    }

    private List<ProfessionalView> withNames(List<Professional> professionals) {
        Map<UUID, AccountName> names = Map.of();
        if (!professionals.isEmpty()) {
            try {
                names = accountDirectory.namesOf(professionals.stream().map(Professional::getIdentityUserId).toList());
            } catch (IdentityUnavailableException unavailable) {
                LOG.warn("professionals returned without account names: {}", unavailable.getMessage());
            }
        }
        Map<UUID, AccountName> found = names;
        return professionals.stream()
                .map(professional -> new ProfessionalView(professional, found.get(professional.getIdentityUserId())))
                .toList();
    }
}
