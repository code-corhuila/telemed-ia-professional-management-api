package com.telemed.professionalmanagement.adapters.identity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A stand-in for the identity service: {@code POST /internal/service-token}
 * (identity-and-access-api #117) and {@code /internal/accounts} (#122, #128). Each request is
 * recorded; the accounts endpoint answers the queued responses in order.
 */
final class IdentityStub implements AutoCloseable {

    record Request(String method, String path, String authorization, String idempotencyKey,
            String correlationId, String contentType, String body, String query) { }

    record Answer(int status, String body) { }

    private final HttpServer server;
    final List<Request> requests = new CopyOnWriteArrayList<>();
    final AtomicInteger tokensIssued = new AtomicInteger();
    private final Deque<Answer> accountAnswers = new ArrayDeque<>();
    volatile int tokenStatus = 200;
    volatile Supplier<Instant> tokenExpiry = () -> Instant.now().plusSeconds(600);

    IdentityStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    URI url() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    synchronized void answerAccounts(int status, String body) {
        accountAnswers.add(new Answer(status, body));
    }

    List<Request> to(String path) {
        return requests.stream().filter(request -> request.path().equals(path)).toList();
    }

    private void handle(HttpExchange exchange) throws IOException {
        var headers = exchange.getRequestHeaders();
        String path = exchange.getRequestURI().getPath();
        requests.add(new Request(exchange.getRequestMethod(), path, headers.getFirst("Authorization"),
                headers.getFirst("Idempotency-Key"), headers.getFirst("X-Correlation-Id"),
                headers.getFirst("Content-Type"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                exchange.getRequestURI().getRawQuery()));
        Answer answer;
        if (path.equals("/internal/service-token")) {
            answer = tokenStatus != 200
                    ? new Answer(tokenStatus, "{\"error\":\"INVALID_CLIENT\",\"message\":\"invalid client\"}")
                    : new Answer(200, "{\"accessToken\":\"token-%d\",\"tokenType\":\"Bearer\",\"expiresAt\":\"%s\"}"
                            .formatted(tokensIssued.incrementAndGet(), tokenExpiry.get()));
        } else {
            synchronized (this) {
                answer = accountAnswers.isEmpty() ? new Answer(500, "{}") : accountAnswers.poll();
            }
        }
        byte[] bytes = answer.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(answer.status(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
