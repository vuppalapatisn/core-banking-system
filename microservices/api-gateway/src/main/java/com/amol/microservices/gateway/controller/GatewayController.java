package com.amol.microservices.gateway.controller;

import com.amol.microservices.gateway.service.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

/**
 * APIM reverse proxy. Any authenticated request to {@code /route/{service}/**} is forwarded to the
 * configured downstream service, after WAF, rate limiting, and JWT/RBAC checks have already run.
 */
@RestController
@Tag(name = "Gateway", description = "Reverse proxy to backend microservices")
public class GatewayController {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ProxyService proxyService;

    public GatewayController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping({"/route/{alias}", "/route/{alias}/**"})
    @Operation(summary = "Proxy a request to a backend service",
            description = "Forwards to the downstream service registered under {alias} "
                    + "(e.g. product, images, ecommerce), preserving method, body, and correlation id.")
    public ResponseEntity<byte[]> proxy(@PathVariable String alias, HttpServletRequest request) throws IOException {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatch = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String downstreamPath = PATH_MATCHER.extractPathWithinPattern(bestMatch, fullPath);
        return proxyService.forward(alias, downstreamPath, request);
    }
}
