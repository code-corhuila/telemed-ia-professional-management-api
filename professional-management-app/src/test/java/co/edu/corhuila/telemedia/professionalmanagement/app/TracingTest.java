package co.edu.corhuila.telemedia.professionalmanagement.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.TestTokens;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * NFR-005: one trace across services. An incoming W3C {@code traceparent} is continued, the
 * request's log line carries {@code traceId} and {@code spanId}, the calls to identity carry the
 * same trace, and spans go to the platform collector over OTLP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
// Spring Boot turns tracing off in tests unless asked for.
@AutoConfigureObservability
@ExtendWith(OutputCaptureExtension.class)
class TracingTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-00f067aa0ba902b7-01";

    /** Stands in for identity; records the trace header of each call. */
    private static final HttpServer IDENTITY = start();
    private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Tracer tracer;

    @Autowired
    private AccountDirectory accounts;

    @Autowired
    private Environment environment;

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        registry.add("JWT_PUBLIC_KEY", TestTokens::trustedPublicKeyPemOneLine);
        registry.add("management.otlp.tracing.export.enabled", () -> "false");
        registry.add("IDENTITY_URL", () -> "http://localhost:" + IDENTITY.getAddress().getPort());
        registry.add("SERVICE_CLIENT_SECRET", () -> "tracing-test-secret");
    }

    @AfterAll
    static void stop() {
        IDENTITY.stop(0);
    }

    @Test
    void anIncomingTraceIsContinuedAndWrittenOnTheLogLine(CapturedOutput output) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("traceparent", TRACEPARENT);

        restTemplate.exchange("/health", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        String accessLine = output.getOut().lines().filter(line -> line.contains("GET /health")).reduce((a, b) -> b)
                .orElseThrow();
        assertThat(accessLine).contains("\"traceId\":\"" + TRACE_ID + "\"").containsPattern("\"spanId\":\"[0-9a-f]{16}\"");
    }

    @Test
    void callsToIdentityCarryTheSameTrace() {
        RECEIVED.clear();
        Span span = tracer.nextSpan().name("list professionals").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            accounts.namesOf(List.of(UUID.randomUUID()));
        } finally {
            span.end();
        }

        // One call for the service token, one for the names.
        assertThat(RECEIVED).hasSize(2).allSatisfy(traceparent -> assertThat(traceparent)
                .startsWith("00-" + span.context().traceId() + "-")
                .doesNotContain(span.context().spanId()));
    }

    @Test
    void everyRequestIsSampledAndExportedToThePlatformCollector() {
        assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("1.0");
        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://otel-collector:4318/v1/traces");
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/internal/service-token", exchange -> answer(exchange,
                    "{\"accessToken\":\"tracing-test-token\",\"tokenType\":\"Bearer\",\"expiresAt\":\""
                            + Instant.now().plusSeconds(600) + "\"}"));
            server.createContext("/internal/accounts", exchange -> answer(exchange, "{\"data\":[]}"));
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void answer(HttpExchange exchange, String body) throws IOException {
        String traceparent = exchange.getRequestHeaders().getFirst("traceparent");
        RECEIVED.add(traceparent == null ? "none" : traceparent);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
