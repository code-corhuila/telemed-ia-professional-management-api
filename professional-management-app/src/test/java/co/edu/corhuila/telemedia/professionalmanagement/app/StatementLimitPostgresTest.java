package co.edu.corhuila.telemedia.professionalmanagement.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Numeral 5.3.10: the pool declared in application.yml gives every connection the statement
 * limit, checked on the real engine. Without {@code TEST_DATABASE_URL} the class is skipped
 * (Anexo I).
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class StatementLimitPostgresTest {

    @DynamicPropertySource
    static void realDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyConnectionCarriesTheStatementLimit() {
        assertThat(jdbc.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("5s");
    }
}
