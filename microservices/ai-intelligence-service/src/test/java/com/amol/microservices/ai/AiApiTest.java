package com.amol.microservices.ai;

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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "server.servlet.context-path=")
class AiApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fraudScoreEndpoint() throws Exception {
        mockMvc.perform(post("/fraud/score")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amountMinor\":1000,\"network\":\"INTERNAL\",\"country\":\"US\","
                                + "\"homeCountry\":\"US\",\"recentTxnCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ALLOW"));
    }

    @Test
    void creditScoreEndpoint() throws Exception {
        mockMvc.perform(post("/credit/score")
                        .contentType(APPLICATION_JSON)
                        .content("{\"annualIncomeMinor\":12000000,\"monthlyDebtMinor\":100000,"
                                + "\"ageYears\":35,\"delinquencies\":0,\"creditUtilizationPct\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.band").exists());
    }

    @Test
    void assistantEndpoint() throws Exception {
        mockMvc.perform(post("/assistant/ask")
                        .contentType(APPLICATION_JSON)
                        .content("{\"question\":\"How do I apply for a loan?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("deterministic-stub"));
    }

    @Test
    void assistantValidatesQuestion() throws Exception {
        mockMvc.perform(post("/assistant/ask")
                        .contentType(APPLICATION_JSON).content("{\"context\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
}
