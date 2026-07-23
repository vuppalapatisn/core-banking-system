package com.amol.microservices.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe configuration for the API Management &amp; Security layer, bound from the
 * {@code gateway.*} properties. Secrets (JWT signing key, tokenization key) are injected
 * from environment variables / Kubernetes secrets — never hardcoded in a shared environment.
 */
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    @NestedConfigurationProperty
    private final Security security = new Security();

    @NestedConfigurationProperty
    private final Tokenization tokenization = new Tokenization();

    @NestedConfigurationProperty
    private final RateLimit rateLimit = new RateLimit();

    @NestedConfigurationProperty
    private final Waf waf = new Waf();

    /** Downstream service routes for the proxy: alias -> base URL (context path included). */
    private Map<String, String> routes = new HashMap<>();

    public Security getSecurity() {
        return security;
    }

    public Tokenization getTokenization() {
        return tokenization;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Waf getWaf() {
        return waf;
    }

    public Map<String, String> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, String> routes) {
        this.routes = routes;
    }

    public static class Security {
        @NestedConfigurationProperty
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }
    }

    public static class Jwt {
        /** HS256 signing secret. Must be at least 32 bytes. */
        private String secret;
        private String issuer = "api-gateway";
        private long ttlSeconds = 3600;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Tokenization {
        /** Base64-encoded AES key (16/24/32 bytes for AES-128/192/256). */
        private String aesKey;

        public String getAesKey() {
            return aesKey;
        }

        public void setAesKey(String aesKey) {
            this.aesKey = aesKey;
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 120;
        private int burst = 40;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getBurst() {
            return burst;
        }

        public void setBurst(int burst) {
            this.burst = burst;
        }
    }

    public static class Waf {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
