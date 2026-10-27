package co.edu.corhuila.telemedia.professionalmanagement.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.TestTokens;

/**
 * NFR-005: Prometheus reads the request rate, errors and latency on the internal management
 * port, without a token; the public port does not serve them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
// Spring Boot turns metrics export off in tests unless asked for.
@AutoConfigureObservability
class MetricsEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @LocalManagementPort
    private int managementPort;

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        registry.add("JWT_PUBLIC_KEY", TestTokens::trustedPublicKeyPemOneLine);
        registry.add("management.otlp.tracing.export.enabled", () -> "false");
    }

    private ResponseEntity<String> scrape() {
        return restTemplate.getForEntity("http://localhost:" + managementPort + "/actuator/prometheus", String.class);
    }

    @Test
    void prometheusReadsTheRequestRateErrorsAndLatencyWithoutAToken() {
        restTemplate.getForEntity("/health", String.class);
        restTemplate.getForEntity("/api/professionals", String.class);

        ResponseEntity<String> response = scrape();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("http_server_requests_seconds_count{")
                .contains("application=\"professional-management-api\"")
                .contains("status=\"200\"")
                .contains("status=\"401\"");
    }

    @Test
    void theHttpLatencyHasTheSloBuckets() {
        restTemplate.getForEntity("/health", String.class);

        assertThat(scrape().getBody()).contains("http_server_requests_seconds_bucket{").contains("le=\"0.5\"");
    }

    @Test
    void thePublicPortDoesNotServeTheMetrics() {
        assertThat(restTemplate.getForEntity("/actuator/prometheus", String.class).getStatusCode())
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    void onlyHealthAndPrometheusAreExposed() {
        assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health,prometheus");
        assertThat(restTemplate.getForEntity("http://localhost:" + managementPort + "/actuator/env", String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
