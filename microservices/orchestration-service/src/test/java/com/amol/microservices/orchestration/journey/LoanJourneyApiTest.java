package com.amol.microservices.orchestration.journey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boundary test for the end-to-end journey with the Core Platforms client mocked (no network). */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "server.servlet.context-path=")
class LoanJourneyApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CorePlatformClient client;

    @Test
    void disbursesApprovedLoan() throws Exception {
        when(client.originateLoan(any(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new CorePlatformClient.Origination("app-1", "ORIGINATED"));
        when(client.bookLoan(anyLong(), anyDouble(), anyInt()))
                .thenReturn(new CorePlatformClient.Booking("loan-1", 12));
        when(client.disburse(any(), any(), anyLong(), any()))
                .thenReturn(new CorePlatformClient.Disbursement("pay-1", "INTERNAL", "SETTLED"));

        mockMvc.perform(post("/journeys/loan")
                        .contentType(APPLICATION_JSON)
                        .content("{\"applicantId\":\"c1\",\"amountMinor\":100000,\"termMonths\":12,"
                                + "\"creditScore\":720,\"annualRatePct\":12.0,\"toAccount\":\"ACC-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DISBURSED"))
                .andExpect(jsonPath("$.loanId").value("loan-1"))
                .andExpect(jsonPath("$.paymentStatus").value("SETTLED"));
    }

    @Test
    void rejectedApplicationReturnsRejectedOutcome() throws Exception {
        when(client.originateLoan(any(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new CorePlatformClient.Origination("app-2", "REJECTED"));

        mockMvc.perform(post("/journeys/loan")
                        .contentType(APPLICATION_JSON)
                        .content("{\"applicantId\":\"c1\",\"amountMinor\":100000,\"termMonths\":12,"
                                + "\"creditScore\":400,\"annualRatePct\":12.0,\"toAccount\":\"ACC-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REJECTED"));
    }

    @Test
    void validatesRequest() throws Exception {
        mockMvc.perform(post("/journeys/loan")
                        .contentType(APPLICATION_JSON)
                        .content("{\"applicantId\":\"c1\",\"amountMinor\":100000,\"termMonths\":12,"
                                + "\"creditScore\":720,\"annualRatePct\":12.0}"))
                .andExpect(status().isBadRequest());
    }
}
