package com.amol.microservices.gateway.filter;

import com.amol.microservices.gateway.config.GatewayProperties;
import com.amol.microservices.gateway.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client rate limiting using an in-memory token bucket (capacity = burst, refilled at
 * requests-per-minute). Exceeding the limit returns HTTP 429 with a {@code Retry-After} header.
 * Clients are keyed by authenticated subject when present, otherwise by remote address.
 *
 * <p>Actuator probe/scrape endpoints are exempt so autoscaling and health checks are never throttled.
 * In-memory state is per-pod; a multi-pod deployment approximates the global limit — acceptable for
 * this reference stack (a distributed store would be the productionization step).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final Counter throttled;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(GatewayProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.throttled = Counter.builder("gateway.ratelimit.throttled")
                .description("Requests rejected by the rate limiter")
                .register(meterRegistry);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        GatewayProperties.RateLimit config = properties.getRateLimit();
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientKey(request);
        TokenBucket bucket = buckets.computeIfAbsent(
                key, k -> new TokenBucket(config.getBurst(), config.getRequestsPerMinute() / 60.0));

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            throttled.increment();
            log.warn("rate_limit_exceeded clientKey={} path={}", key, request.getRequestURI());
            writeTooManyRequests(response);
        }
    }

    private static String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "1");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse("rate_limited", "Rate limit exceeded; retry later"));
    }

    /** Thread-safe token bucket. Refills continuously at {@code refillPerSecond} up to {@code capacity}. */
    static final class TokenBucket {
        private final double capacity;
        private final double refillPerSecond;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(double capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            if (elapsedSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
                lastRefillNanos = now;
            }
        }
    }
}
