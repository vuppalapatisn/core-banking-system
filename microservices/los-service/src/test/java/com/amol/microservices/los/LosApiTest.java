package com.amol.microservices.los;

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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "server.servlet.context-path=")
class LosApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String submit(int creditScore, long amountMinor) throws Exception {
        String body = mockMvc.perform(post("/applications")
                        .contentType(APPLICATION_JSON)
                        .content("{\"applicantId\":\"c1\",\"amountMinor\":" + amountMinor
                                + ",\"termMonths\":24,\"creditScore\":" + creditScore + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void underwriteApproveThenOriginate() throws Exception {
        String id = submit(720, 1_000_000);
        mockMvc.perform(post("/applications/" + id + "/underwrite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/applications/" + id + "/originate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORIGINATED"));
    }

    @Test
    void lowScoreRejected() throws Exception {
        String id = submit(500, 1_000_000);
        mockMvc.perform(post("/applications/" + id + "/underwrite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void originateBeforeApprovalIsConflict() throws Exception {
        String id = submit(720, 1_000_000);
        mockMvc.perform(post("/applications/" + id + "/originate"))
                .andExpect(status().isConflict());
    }

    @Test
    void submitValidatesInput() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(APPLICATION_JSON)
                        .content("{\"applicantId\":\"c1\",\"amountMinor\":-5,\"termMonths\":24,\"creditScore\":700}"))
                .andExpect(status().isBadRequest());
    }
}
