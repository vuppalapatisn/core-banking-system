package com.amol.microservices.events;

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
@TestPropertySource(properties = "server.servlet.context-path=")
class EventsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPublishPollAndCdc() throws Exception {
        mockMvc.perform(post("/topics")
                        .contentType(APPLICATION_JSON).content("{\"name\":\"orders\",\"partitions\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partitions").value(3));

        mockMvc.perform(post("/topics/orders/publish")
                        .contentType(APPLICATION_JSON).content("{\"key\":\"k1\",\"payload\":{\"amount\":100}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset").value(0));

        mockMvc.perform(get("/topics/orders/poll").param("group", "g1").param("max", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(get("/topics/orders/lag").param("group", "g2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lag").value(1));

        mockMvc.perform(post("/cdc")
                        .contentType(APPLICATION_JSON)
                        .content("{\"entity\":\"account\",\"changeType\":\"UPDATE\",\"key\":\"a1\","
                                + "\"after\":{\"balance\":500}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("cdc.account"));
    }

    @Test
    void publishValidatesPayload() throws Exception {
        mockMvc.perform(post("/topics/orders/publish")
                        .contentType(APPLICATION_JSON).content("{\"key\":\"k1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTopicValidatesName() throws Exception {
        mockMvc.perform(post("/topics")
                        .contentType(APPLICATION_JSON).content("{\"partitions\":3}"))
                .andExpect(status().isBadRequest());
    }
}
