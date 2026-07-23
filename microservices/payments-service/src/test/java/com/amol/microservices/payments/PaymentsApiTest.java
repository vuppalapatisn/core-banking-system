package com.amol.microservices.payments;

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
class PaymentsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submitPaymentSettles() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"k1\",\"fromAccount\":\"ACC-1\","
                                + "\"toAccount\":\"ACC-2\",\"amountMinor\":5000,\"currency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.network").value("INTERNAL"));
    }

    @Test
    void largePaymentRoutesToWire() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"fromAccount\":\"ACC-1\",\"toAccount\":\"ACC-2\",\"amountMinor\":2000000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.network").value("WIRE"));
    }

    @Test
    void validationRejectsMissingAccounts() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amountMinor\":5000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameFromAndToIsBadRequest() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"fromAccount\":\"ACC-1\",\"toAccount\":\"ACC-1\",\"amountMinor\":5000}"))
                .andExpect(status().isBadRequest());
    }
}
