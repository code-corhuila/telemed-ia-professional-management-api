package co.edu.corhuila.telemedia.professionalmanagement.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountPort;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import co.edu.corhuila.telemedia.professionalmanagement.application.usecase.ProfessionalApplicationService;
import co.edu.corhuila.telemedia.professionalmanagement.application.usecase.ProfessionalRegistrationService;
import co.edu.corhuila.telemedia.professionalmanagement.application.usecase.ProfessionalStatusService;

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

    @Bean
    ProfessionalRegistrationService professionalRegistration(IdentityAccountPort identityAccounts,
            ProfessionalRegistrationStore store) {
        return new ProfessionalRegistrationService(identityAccounts, store);
    }

    @Bean
    ProfessionalStatusService professionalStatus(ProfessionalRepositoryPort professionals) {
        return new ProfessionalStatusService(professionals);
    }
}
