package co.edu.corhuila.telemedia.professionalmanagement.app;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ChangeProfessionalStatusUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.CreateProfessionalProfileUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.RegisterProfessionalAccountUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountPort;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import co.edu.corhuila.telemedia.professionalmanagement.application.usecase.ProfessionalApplicationService;
import co.edu.corhuila.telemedia.professionalmanagement.application.usecase.ProfessionalProfileService;
import co.edu.corhuila.telemedia.professionalmanagement.application.usecase.ProfessionalRegistrationService;
import co.edu.corhuila.telemedia.professionalmanagement.application.usecase.ProfessionalStatusService;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Wires the use cases to their ports (Anexo C, composition root). The use cases carry no
 * framework annotation (numeral 5.3.3).
 */
@Configuration
public class ProfessionalManagementConfiguration {

    @Bean
    ProfessionalApplicationService professionalQueries(ProfessionalRepositoryPort professionals,
            AccountDirectory accountDirectory) {
        return new ProfessionalApplicationService(professionals, accountDirectory);
    }

    /** Each registration leaves a domain event in the log and counts its outcome (NFR-005). */
    @Bean
    RegisterProfessionalAccountUseCase professionalRegistration(IdentityAccountPort identityAccounts,
            ProfessionalRegistrationStore store, MeterRegistry meters) {
        return new ObservedRegistration(new ProfessionalRegistrationService(identityAccounts, store), meters);
    }

    /** Each status change leaves a domain event in the log and counts its outcome (NFR-005). */
    @Bean
    ChangeProfessionalStatusUseCase professionalStatus(ProfessionalRepositoryPort professionals,
            @Value("${workflow.service-clients}") Set<String> workflowClients, MeterRegistry meters) {
        return new ObservedStatusChange(new ProfessionalStatusService(professionals, workflowClients), meters);
    }

    /** Step 2 of the onboarding saga of telemed-ia-workflow (DEC-015). */
    @Bean
    CreateProfessionalProfileUseCase professionalProfiles(ProfessionalRegistrationStore store,
            @Value("${workflow.service-clients}") Set<String> workflowClients) {
        return new ProfessionalProfileService(store, workflowClients);
    }
}
