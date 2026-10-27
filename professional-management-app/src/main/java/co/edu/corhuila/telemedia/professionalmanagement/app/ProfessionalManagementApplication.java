package co.edu.corhuila.telemedia.professionalmanagement.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition root of the Professional Management API (Anexo C). Component scanning starts at
 * the service base package so the adapters are wired in.
 */
@SpringBootApplication(scanBasePackages = "co.edu.corhuila.telemedia.professionalmanagement")
public class ProfessionalManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfessionalManagementApplication.class, args);
    }
}
