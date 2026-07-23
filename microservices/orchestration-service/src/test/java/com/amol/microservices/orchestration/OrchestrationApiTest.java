package com.amol.microservices.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boundary tests over the real config-driven rules, exercising each orchestration API through the
 * full HTTP stack.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "server.servlet.context-path=")
class OrchestrationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rulesEvaluateReturnsMatches() throws Exception {
        mockMvc.perform(post("/rules/evaluate")
                        .contentType(APPLICATION_JSON)
                        .content("{\"ruleSet\":\"credit\",\"facts\":{\"creditScore\":550}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(1));
    }

    @Test
    void rulesEvaluateValidatesInput() throws Exception {
        mockMvc.perform(post("/rules/evaluate")
                        .contentType(APPLICATION_JSON)
                        .content("{\"facts\":{\"creditScore\":550}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void decisionRejectsLowCreditScore() throws Exception {
        mockMvc.perform(post("/decisions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"facts\":{\"creditScore\":500,\"debtToIncome\":0.2,\"creditHistoryYears\":5}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REJECT"));
    }

    @Test
    void loanWorkflowRunsToCompletion() throws Exception {
        String started = mockMvc.perform(post("/workflows")
                        .contentType(APPLICATION_JSON)
                        .content("{\"type\":\"loan-approval\",\"facts\":{\"creditScore\":720,"
                                + "\"debtToIncome\":0.2,\"creditHistoryYears\":5,\"amount\":5000}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(started).get("id").asText();

        mockMvc.perform(post("/workflows/" + id + "/advance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("APPROVED"));

        mockMvc.perform(post("/workflows/" + id + "/advance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DISBURSED"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void integrationRoutesConfiguredMessage() throws Exception {
        mockMvc.perform(post("/integration/route")
                        .contentType(APPLICATION_JSON)
                        .content("{\"domain\":\"loan\",\"type\":\"disbursement\",\"payload\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("payments-switch"))
                .andExpect(jsonPath("$.dispatched").value(true));
    }

    @Test
    void integrationRejectsUnknownRoute() throws Exception {
        mockMvc.perform(post("/integration/route")
                        .contentType(APPLICATION_JSON)
                        .content("{\"domain\":\"loan\",\"type\":\"nope\",\"payload\":{}}"))
                .andExpect(status().isBadRequest());
    }
}
