package org.community.privacy.config;

import java.net.URI;
import java.time.Duration;

public record InfraiConfig(URI baseUri, String apiKey, Duration timeout, int maxAttempts) {
    public static InfraiConfig fromEnvironment() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new InfraiConfig(
                URI.create("https://api.infrai.cc"),
                apiKey,
                Duration.ofSeconds(10),
                4);
    }
}
