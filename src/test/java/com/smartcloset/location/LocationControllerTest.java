package com.smartcloset.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class LocationControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void returnsNineRepresentativeLocations() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        assertThat(data).hasSize(9);
        assertThat(data.get(0).get("code").asText()).isEqualTo("SEOUL");
        assertThat(data.get(0).get("name").asText()).isEqualTo("서울특별시");
        assertThat(data.get(0).get("nx").asInt()).isEqualTo(60);
        assertThat(data.get(0).get("ny").asInt()).isEqualTo(127);
    }

    @Test
    void searchesLocationsByKoreanName() throws Exception {
        mockMvc.perform(get("/api/locations")
                        .param("keyword", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("SEOUL"))
                .andExpect(jsonPath("$.data[0].name").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].nx").value(60))
                .andExpect(jsonPath("$.data[0].ny").value(127));
    }

    @Test
    void searchesLocationsByCodeCaseInsensitively() throws Exception {
        mockMvc.perform(get("/api/locations")
                        .param("keyword", "SEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("SEOUL"));
    }
}
