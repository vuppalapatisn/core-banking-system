package com.amol.microservices.gateway.service;

import com.amol.microservices.gateway.config.GatewayProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * The APIM reverse proxy. Forwards an authenticated request to the configured downstream service,
 * propagating the {@code X-Correlation-Id} so a request can be traced end-to-end, and relaying the
 * downstream status, headers, and body back to the caller.
 */
@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    /** Request headers we never forward (hop-by-hop, host, or the gateway's own credentials). */
    private static final Set<String> STRIPPED_REQUEST_HEADERS = Set.of(
            "host", "authorization", "connection", "content-length", "transfer-encoding");

    /** Response headers we never relay (the servlet container recomputes framing/length). */
    private static final Set<String> STRIPPED_RESPONSE_HEADERS = Set.of(
            "connection", "content-length", "transfer-encoding");

    private final GatewayProperties properties;
    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    public ProxyService(GatewayProperties properties, RestClient.Builder builder, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.restClient = builder.requestFactory(factory).build();
    }

    /**
     * Forwards {@code request} to {@code alias}'s downstream base URL under {@code downstreamPath}.
     *
     * @throws IllegalArgumentException if the alias is not a configured route (maps to HTTP 400)
     */
    public ResponseEntity<byte[]> forward(String alias, String downstreamPath, HttpServletRequest request)
            throws IOException {
        String baseUrl = properties.getRoutes().get(alias);
        if (baseUrl == null) {
            throw new IllegalArgumentException("Unknown route: " + alias);
        }

        URI targetUri = buildTargetUri(baseUrl, downstreamPath, request.getQueryString());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        String correlationId = MDC.get("correlationId");

        try {
            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(targetUri)
                    .headers(headers -> copyRequestHeaders(request, headers, correlationId));
            if (body.length > 0) {
                spec.body(body);
            }
            ResponseEntity<byte[]> response = spec.exchange((req, res) -> {
                HttpHeaders relayed = new HttpHeaders();
                res.getHeaders().forEach((name, values) -> {
                    if (!STRIPPED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                        relayed.addAll(name, values);
                    }
                });
                byte[] responseBody = StreamUtils.copyToByteArray(res.getBody());
                return ResponseEntity.status(res.getStatusCode()).headers(relayed).body(responseBody);
            }, true);

            counter(alias, "success").increment();
            return response;
        } catch (ResourceAccessException ex) {
            counter(alias, "upstream_unavailable").increment();
            log.warn("proxy_upstream_unavailable route={} reason={}", alias, ex.getMessage());
            byte[] payload = "{\"error\":\"upstream_unavailable\",\"message\":\"Downstream service unavailable\"}"
                    .getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.status(502)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(payload);
        }
    }

    private URI buildTargetUri(String baseUrl, String downstreamPath, String queryString) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (downstreamPath != null && !downstreamPath.isBlank()) {
            if (!baseUrl.endsWith("/") && !downstreamPath.startsWith("/")) {
                url.append('/');
            }
            url.append(downstreamPath);
        }
        if (queryString != null && !queryString.isBlank()) {
            url.append('?').append(queryString);
        }
        return URI.create(url.toString());
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpHeaders headers, String correlationId) {
        var names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!STRIPPED_REQUEST_HEADERS.contains(name.toLowerCase())) {
                var values = request.getHeaders(name);
                while (values.hasMoreElements()) {
                    headers.add(name, values.nextElement());
                }
            }
        }
        if (correlationId != null) {
            headers.set(CORRELATION_HEADER, correlationId);
        }
    }

    private Counter counter(String alias, String outcome) {
        return Counter.builder("gateway.proxy.requests")
                .tag("route", alias)
                .tag("outcome", outcome)
                .description("Requests proxied to downstream services")
                .register(meterRegistry);
    }
}
