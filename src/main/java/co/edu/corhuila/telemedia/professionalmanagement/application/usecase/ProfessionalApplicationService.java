package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.GetProfessionalUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Page;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalView;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalNotFoundException;

/**
 * Reads the professionals with the names of their accounts (DEC-008): one identity call per
 * page, and the professionals are returned without names when identity cannot answer. The
 * account e-mail is kept only for administrators (DEC-009).
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
    public Page<ProfessionalView> list(Caller caller, PageRequest page) {
        Page<Professional> stored = professionalRepositoryPort.findPage(page);
        return new Page<>(withNames(caller, stored.items()), stored.page(), stored.limit(), stored.total());
    }

    @Override
    public ProfessionalView getById(Caller caller, UUID professionalId) {
        Professional professional = professionalRepositoryPort.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
        return withNames(caller, List.of(professional)).get(0);
    }

    private List<ProfessionalView> withNames(Caller caller, List<Professional> professionals) {
        Map<UUID, AccountName> names = Map.of();
        if (!professionals.isEmpty()) {
            try {
                names = accountDirectory.namesOf(professionals.stream().map(Professional::getIdentityUserId).toList());
            } catch (IdentityUnavailableException unavailable) {
                LOG.warn("professionals returned without account names: {}", unavailable.getMessage());
            }
        }
        Map<UUID, AccountName> found = names;
        boolean withEmail = caller.hasRole(Caller.ADMIN);
        return professionals.stream()
                .map(professional -> new ProfessionalView(professional,
                        visibleTo(withEmail, found.get(professional.getIdentityUserId()))))
                .toList();
    }

    /** DEC-009: the e-mail is personal data that only the administrator needs. */
    private static AccountName visibleTo(boolean withEmail, AccountName account) {
        return account == null || withEmail ? account : new AccountName(account.fullName(), null);
    }
}
