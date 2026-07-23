package com.amol.microservices.lms;

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
class LmsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String bookLoan() throws Exception {
        String body = mockMvc.perform(post("/loans")
                        .contentType(APPLICATION_JSON)
                        .content("{\"principalMinor\":100000,\"annualRatePct\":12.0,\"termMonths\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingMinor").value(100000))
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void scheduleHasTwelveInstallments() throws Exception {
        String id = bookLoan();
        mockMvc.perform(get("/loans/" + id + "/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void repaymentPaysOffLoan() throws Exception {
        String id = bookLoan();
        mockMvc.perform(post("/loans/" + id + "/payments")
                        .contentType(APPLICATION_JSON).content("{\"amountMinor\":100000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingMinor").value(0))
                .andExpect(jsonPath("$.state").value("PAID_OFF"));
    }

    @Test
    void bookValidatesInput() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(APPLICATION_JSON)
                        .content("{\"principalMinor\":0,\"annualRatePct\":12.0,\"termMonths\":12}"))
                .andExpect(status().isBadRequest());
    }
}
