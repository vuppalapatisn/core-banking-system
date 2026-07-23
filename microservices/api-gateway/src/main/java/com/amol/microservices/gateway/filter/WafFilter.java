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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lightweight Web Application Firewall. Inspects the request line (path + query string) for common
 * SQL-injection and cross-site-scripting signatures and rejects matches with HTTP 403 before they
 * reach authentication or any downstream service.
 *
 * <p>Signature-based and deliberately conservative to limit false positives; it complements, and
 * does not replace, per-endpoint input validation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class WafFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WafFilter.class);

    private static final List<Pattern> SIGNATURES = List.of(
            // SQL injection
            Pattern.compile("('|%27)\\s*(or|and)\\s", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(union\\s+select|select\\s+.*\\s+from|insert\\s+into|drop\\s+table|"
                    + "delete\\s+from|update\\s+.*\\s+set)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(;|--|#|/\\*|\\*/)\\s*(drop|select|insert|update|delete|union)",
                    Pattern.CASE_INSENSITIVE),
            // Cross-site scripting
            Pattern.compile("<\\s*script", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on(error|load|click|mouseover)\\s*=", Pattern.CASE_INSENSITIVE),
            // Path traversal
            Pattern.compile("\\.\\./|\\.\\.%2f", Pattern.CASE_INSENSITIVE));

    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final Counter blocked;

    public WafFilter(GatewayProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.blocked = Counter.builder("gateway.waf.blocked")
                .description("Requests blocked by the WAF")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (properties.getWaf().isEnabled()) {
            String candidate = decode(request.getRequestURI())
                    + " " + decode(request.getQueryString());
            for (Pattern signature : SIGNATURES) {
                if (signature.matcher(candidate).find()) {
                    blocked.increment();
                    log.warn("waf_blocked method={} path={} signature={}",
                            request.getMethod(), request.getRequestURI(), signature.pattern());
                    writeForbidden(response);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String decode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // Malformed percent-encoding — inspect the raw form rather than trusting it.
            return value;
        }
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse("request_blocked", "Request blocked by web application firewall"));
    }
}
