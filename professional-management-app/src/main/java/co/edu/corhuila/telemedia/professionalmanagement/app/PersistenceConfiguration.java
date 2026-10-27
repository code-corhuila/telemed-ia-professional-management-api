package co.edu.corhuila.telemedia.professionalmanagement.app;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The JPA entities and repositories live in the persistence adapter, outside the package of the
 * application class. Kept apart from it so the web slice tests do not start JPA.
 */
@Configuration
@EntityScan(PersistenceConfiguration.PERSISTENCE)
@EnableJpaRepositories(PersistenceConfiguration.PERSISTENCE)
public class PersistenceConfiguration {

    static final String PERSISTENCE = "co.edu.corhuila.telemedia.professionalmanagement.adapter.out.persistence";
}
