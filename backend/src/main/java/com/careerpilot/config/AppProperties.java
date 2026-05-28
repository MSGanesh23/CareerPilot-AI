package com.careerpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final FileStorage file = new FileStorage();
    private final Ai ai = new Ai();
    private final Cors cors = new Cors();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs;
        private long refreshExpirationMs;
    }

    @Data
    public static class FileStorage {
        private String uploadDir;
        private String allowedTypes;
        private long maxSizeBytes;
    }

    @Data
    public static class Ai {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String model;
        private int maxTokens;
        private int timeoutSeconds;
    }

    @Data
    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods;
        private long maxAge;
    }
}
