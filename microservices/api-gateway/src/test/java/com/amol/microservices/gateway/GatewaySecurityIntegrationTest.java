package com.amol.microservices.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boundary tests for the security posture: public vs authenticated vs role-restricted routes,
 * exercised through the full servlet filter chain (WAF, rate limit, JWT/RBAC).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "server.servlet.context-path=")
class GatewaySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenFor(String clientId, String clientSecret) throws Exception {
        String body = mockMvc.perform(post("/auth/token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientId\":\"" + clientId + "\",\"clientSecret\":\"" + clientSecret + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void tokenizeRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/security/tokenize")
                        .contentType(APPLICATION_JSON)
                        .content("{\"value\":\"4111111111111111\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenEndpointIssuesJwtForValidClient() throws Exception {
        // Uses the dev-default service client credentials.
        String token = tokenFor("gateway-service", "dev-service-secret-change-me");
        org.assertj.core.api.Assertions.assertThat(token).isNotBlank();
    }

    @Test
    void authenticatedUserCanTokenize() throws Exception {
        String token = tokenFor("gateway-service", "dev-service-secret-change-me");
        mockMvc.perform(post("/security/tokenize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"value\":\"4111111111111111\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void detokenizeIsForbiddenForNonAdmin() throws Exception {
        String token = tokenFor("gateway-service", "dev-service-secret-change-me");
        mockMvc.perform(post("/security/detokenize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"tok_whatever\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientId\":\"gateway-service\",\"clientSecret\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
