package org.community.privacy.infrai;

import org.community.privacy.config.InfraiConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class InfraiAccountClient implements AccountAccessGateway {
    private final InfraiConfig config;
    private final HttpClient http;

    public InfraiAccountClient(InfraiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.timeout()).build());
    }

    InfraiAccountClient(InfraiConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public List<String> listSessionIds(String userId) {
        Object data = call("GET", "/v1/auth/session/list_for_user/" + segment(userId));
        if (data instanceof List<?> sessions) {
            return sessions.stream().map(this::sessionId).toList();
        }
        if (data instanceof Map<?, ?> map && map.get("sessions") instanceof List<?> sessions) {
            return sessions.stream().map(this::sessionId).toList();
        }
        throw new IllegalStateException("Session data has an unexpected shape");
    }

    @Override
    public void revokeSession(String sessionId) {
        call("POST", "/v1/auth/session/revoke/" + segment(sessionId),
                "{\"session_id\":" + jsonString(sessionId) + "}");
    }

    @Override
    public void revokeCredential(String credentialId) {
        call("DELETE", "/v1/account/keys/revoke/" + segment(credentialId));
    }

    private Object call(String method, String path) {
        return call(method, path, null);
    }

    private Object call(String method, String path, String body) {
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(config.baseUri().resolve(path))
                    .timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (body != null) requestBuilder.header("Content-Type", "application/json");
            HttpRequest request = requestBuilder.method(method, body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new IllegalStateException("Infrai transport failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai call interrupted", e);
            }

            Map<String, Object> envelope = envelope(response.body());
            if (response.statusCode() == 429 && attempt < config.maxAttempts()) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Map<String, Object> error = object(envelope.get("error"), "error");
                throw new InfraiException(String.valueOf(error.getOrDefault("code", "INFRAI_REJECTED")), error,
                        response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Infrai transport status " + response.statusCode());
            }
            return envelope.get("data");
        }
        throw new IllegalStateException("Infrai retry budget exhausted");
    }

    private Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Duration.ofSeconds(Long.parseLong(value)))
                .orElse(Duration.ofMillis(200L * (1L << (attempt - 1))));
    }

    private void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }

    private String sessionId(Object entry) {
        if (entry instanceof String id) return id;
        return String.valueOf(object(entry, "session").get("id"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> envelope(String body) {
        return object(Json.parse(body), "envelope");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value, String name) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalStateException("Expected " + name + " object");
        return (Map<String, Object>) value;
    }

    private String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String jsonString(String value) {
        StringBuilder encoded = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> encoded.append("\\\"");
                case '\\' -> encoded.append("\\\\");
                case '\b' -> encoded.append("\\b");
                case '\f' -> encoded.append("\\f");
                case '\n' -> encoded.append("\\n");
                case '\r' -> encoded.append("\\r");
                case '\t' -> encoded.append("\\t");
                default -> {
                    if (c < 0x20) encoded.append(String.format("\\u%04x", (int) c));
                    else encoded.append(c);
                }
            }
        }
        return encoded.append('"').toString();
    }
}
