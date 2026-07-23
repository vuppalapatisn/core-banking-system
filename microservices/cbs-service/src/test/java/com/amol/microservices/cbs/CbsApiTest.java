package com.amol.microservices.cbs;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"server.servlet.context-path=", "events.enabled=false"})
class CbsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createCustomer() throws Exception {
        String body = mockMvc.perform(post("/customers")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"email\":\"ada@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String openAccount(String customerId) throws Exception {
        String body = mockMvc.perform(post("/accounts")
                        .contentType(APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"type\":\"SAVINGS\",\"currency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinor").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void depositAndWithdrawFlow() throws Exception {
        String accountId = openAccount(createCustomer());

        mockMvc.perform(post("/accounts/" + accountId + "/deposit")
                        .contentType(APPLICATION_JSON).content("{\"amountMinor\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinor").value(10000));

        mockMvc.perform(post("/accounts/" + accountId + "/withdraw")
                        .contentType(APPLICATION_JSON).content("{\"amountMinor\":4000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinor").value(6000));

        mockMvc.perform(get("/accounts/" + accountId + "/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void insufficientFundsReturnsConflict() throws Exception {
        String accountId = openAccount(createCustomer());
        mockMvc.perform(post("/accounts/" + accountId + "/withdraw")
                        .contentType(APPLICATION_JSON).content("{\"amountMinor\":5000}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createCustomerValidatesEmail() throws Exception {
        mockMvc.perform(post("/customers")
                        .contentType(APPLICATION_JSON).content("{\"name\":\"NoEmail\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAccountReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/accounts/does-not-exist"))
                .andExpect(status().isBadRequest());
    }
}
