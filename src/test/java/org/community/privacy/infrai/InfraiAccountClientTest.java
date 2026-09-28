package org.community.privacy.infrai;

import com.sun.net.httpserver.HttpServer;
import org.community.privacy.config.InfraiConfig;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public final class InfraiAccountClientTest {
    public static void main(String[] args) throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/auth/session/revoke/session-1", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"ok\":true,\"data\":null}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            InfraiConfig config = new InfraiConfig(
                    java.net.URI.create("http://localhost:" + server.getAddress().getPort()),
                    "test-key", Duration.ofSeconds(2), 1);
            new InfraiAccountClient(config).revokeSession("session-1");
            check("{\"session_id\":\"session-1\"}".equals(body.get()),
                    "revoke request must include the required session_id field");
        } finally {
            server.stop(0);
        }
        System.out.println("PASS: session revoke includes session_id request field");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
