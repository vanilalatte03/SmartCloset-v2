package com.smartcloset.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void locationsRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsKmaCatalogWithMvp7LocationShape() throws Exception {
        User user = userRepository.save(User.createSeedUser("location-catalog-user"));

        MvcResult result = mockMvc.perform(get("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        assertThat(data).hasSizeGreaterThan(9);
        assertThat(data.get(0).get("code").asText()).isEqualTo("SEOUL");
        assertThat(data.get(0).get("name").asText()).isEqualTo("서울특별시");
        assertThat(data.get(0).get("fullName").asText()).isEqualTo("서울특별시");
        assertThat(data.get(0).get("region1").asText()).isEqualTo("서울특별시");
        assertThat(data.get(0).get("region2").isNull()).isTrue();
        assertThat(data.get(0).get("region3").isNull()).isTrue();
        assertThat(data.get(0).get("nx").asInt()).isEqualTo(60);
        assertThat(data.get(0).get("ny").asInt()).isEqualTo(127);
        assertThat(data.get(0).get("latitude").isNull()).isFalse();
        assertThat(data.get(0).get("longitude").isNull()).isFalse();
    }

    @Test
    void searchesLocationsByKoreanName() throws Exception {
        User user = userRepository.save(User.createSeedUser("location-korean-search-user"));

        mockMvc.perform(get("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .param("keyword", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("SEOUL"))
                .andExpect(jsonPath("$.data[0].name").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].fullName").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].region1").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].region2").doesNotExist())
                .andExpect(jsonPath("$.data[0].region3").doesNotExist())
                .andExpect(jsonPath("$.data[0].nx").value(60))
                .andExpect(jsonPath("$.data[0].ny").value(127))
                .andExpect(jsonPath("$.data[0].latitude").exists())
                .andExpect(jsonPath("$.data[0].longitude").exists());
    }

    @Test
    void searchesLocationsByCodeCaseInsensitively() throws Exception {
        User user = userRepository.save(User.createSeedUser("location-code-search-user"));

        mockMvc.perform(get("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .param("keyword", "SEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("SEOUL"));
    }

    @Test
    void searchesIlsanDongWithMultipleKmaAdministrativeCandidates() throws Exception {
        User user = userRepository.save(User.createSeedUser("location-ilsan-search-user"));

        mockMvc.perform(get("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .param("keyword", "일산동"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data[0].code").value("KMA_4128551000"))
                .andExpect(jsonPath("$.data[0].name").value("일산1동"))
                .andExpect(jsonPath("$.data[0].fullName").value("경기도 고양시일산서구 일산1동"))
                .andExpect(jsonPath("$.data[0].region1").value("경기도"))
                .andExpect(jsonPath("$.data[0].region2").value("고양시일산서구"))
                .andExpect(jsonPath("$.data[0].region3").value("일산1동"))
                .andExpect(jsonPath("$.data[0].nx").value(56))
                .andExpect(jsonPath("$.data[0].ny").value(129))
                .andExpect(jsonPath("$.data[0].latitude").value(37.6821))
                .andExpect(jsonPath("$.data[0].longitude").value(126.7698));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }
}
