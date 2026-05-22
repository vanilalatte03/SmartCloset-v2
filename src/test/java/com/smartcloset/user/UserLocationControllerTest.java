package com.smartcloset.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserLocationControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void returnsSeedUserDefaultSeoulLocation() throws Exception {
        mockMvc.perform(get("/api/users/location")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.code").value("SEOUL"))
                .andExpect(jsonPath("$.data.name").value("서울특별시"))
                .andExpect(jsonPath("$.data.nx").value(60))
                .andExpect(jsonPath("$.data.ny").value(127))
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    void backfillsAndPersistsDefaultSeoulLocationForLegacyUser() throws Exception {
        User legacyUser = userRepository.save(User.create("legacy-user"));
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(legacyUser.getId()).orElseThrow().hasLocation()).isFalse();

        mockMvc.perform(get("/api/users/location")
                        .param("userId", legacyUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(legacyUser.getId()))
                .andExpect(jsonPath("$.data.code").value("SEOUL"))
                .andExpect(jsonPath("$.data.name").value("서울특별시"))
                .andExpect(jsonPath("$.data.nx").value(60))
                .andExpect(jsonPath("$.data.ny").value(127));

        entityManager.flush();
        entityManager.clear();

        User saved = userRepository.findById(legacyUser.getId()).orElseThrow();
        assertThat(saved.getLocationCode()).isEqualTo("SEOUL");
        assertThat(saved.getLocationName()).isEqualTo("서울특별시");
        assertThat(saved.getLocationNx()).isEqualTo(60);
        assertThat(saved.getLocationNy()).isEqualTo(127);
    }

    @Test
    void updatesUserLocationToSelectedCatalogLocation() throws Exception {
        Map<String, Object> request = Map.of("locationCode", "BUSAN");

        mockMvc.perform(put("/api/users/location")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.code").value("BUSAN"))
                .andExpect(jsonPath("$.data.name").value("부산광역시"))
                .andExpect(jsonPath("$.data.nx").value(98))
                .andExpect(jsonPath("$.data.ny").value(76))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        entityManager.flush();
        entityManager.clear();

        User saved = userRepository.findById(1L).orElseThrow();
        assertThat(saved.getLocationCode()).isEqualTo("BUSAN");
        assertThat(saved.getLocationName()).isEqualTo("부산광역시");
        assertThat(saved.getLocationNx()).isEqualTo(98);
        assertThat(saved.getLocationNy()).isEqualTo(76);
    }

    @Test
    void returnsLocationNotFoundForUnknownLocationCode() throws Exception {
        Map<String, Object> request = Map.of("locationCode", "UNKNOWN");

        mockMvc.perform(put("/api/users/location")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("위치를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.details[0].field").value("locationCode"))
                .andExpect(jsonPath("$.details[0].message").value("UNKNOWN"));
    }

    @Test
    void returnsUserNotFoundWithExistingErrorShape() throws Exception {
        mockMvc.perform(get("/api/users/location")
                        .param("userId", "99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.details").isArray());
    }
}
