package com.telemed.professionalmanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * Numeral 5.3.10 (Anexo C, "Límites explícitos"): every limit is declared with its value in
 * the configuration of the composition root, not left to the framework defaults.
 */
class ExplicitLimitsTest {

    private static Properties declared;

    @BeforeAll
    static void readApplicationYaml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        declared = yaml.getObject();
    }

    @Test
    void theServerLimitsAreDeclared() {
        assertThat(declared.getProperty("server.tomcat.connection-timeout")).isEqualTo("5s");
        assertThat(declared.getProperty("server.tomcat.keep-alive-timeout")).isEqualTo("60s");
        assertThat(declared.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(declared.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("20s");
    }

    @Test
    void theDatabaseLimitsAreDeclared() {
        assertThat(declared.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("10");
        assertThat(declared.getProperty("spring.datasource.hikari.connection-timeout")).isEqualTo("5000");
        assertThat(declared.getProperty("spring.datasource.hikari.connection-init-sql"))
                .isEqualTo("SET statement_timeout = '5s'");
    }

    @Test
    void theIdentityCallsAreBounded() {
        assertThat(declared.getProperty("identity.request-timeout")).isEqualTo("${IDENTITY_REQUEST_TIMEOUT:5s}");
    }
}
