package com.amol.microservices.gateway.filter;

import com.amol.microservices.gateway.config.GatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class WafFilterTest {

    private final WafFilter filter =
            new WafFilter(new GatewayProperties(), new ObjectMapper(), new SimpleMeterRegistry());

    private MockHttpServletResponse run(String uri, String query) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setQueryString(query);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        request.setAttribute("chainInvoked", chain.getRequest() != null);
        return response;
    }

    @Test
    void allowsBenignRequest() throws Exception {
        MockHttpServletResponse response = run("/api-gateway/route/product/products", "category=books");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksSqlInjection() throws Exception {
        MockHttpServletResponse response = run("/api-gateway/route/product/products", "name=' or 1=1");
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("request_blocked");
    }

    @Test
    void blocksUnionSelect() throws Exception {
        MockHttpServletResponse response = run("/api-gateway/route/product/products", "q=1 union select password from users");
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void blocksXss() throws Exception {
        MockHttpServletResponse response = run("/api-gateway/route/product/search", "q=<script>alert(1)</script>");
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void blocksPathTraversal() throws Exception {
        MockHttpServletResponse response = run("/api-gateway/route/product/../../etc/passwd", null);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsWhenDisabled() throws Exception {
        GatewayProperties props = new GatewayProperties();
        props.getWaf().setEnabled(false);
        WafFilter disabled = new WafFilter(props, new ObjectMapper(), new SimpleMeterRegistry());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-gateway/route/product");
        request.setQueryString("name=' or 1=1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        disabled.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
