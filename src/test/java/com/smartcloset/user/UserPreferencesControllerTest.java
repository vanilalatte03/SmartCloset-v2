package com.smartcloset.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.domain.UserRole;
import com.smartcloset.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserPreferencesControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void userPreferencesApisRequireBearerToken() throws Exception {
        Map<String, Object> request = Map.of(
                "preferredColors", List.of("NAVY"),
                "preferredMaterials", List.of("COTTON"),
                "styleTags", List.of("MINIMAL")
        );

        mockMvc.perform(get("/api/users/me/preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(put("/api/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsDefaultEmptyPreferencesWithoutUserIdField() throws Exception {
        User user = userRepository.save(User.createSeedUser("preferences-default-user"));

        mockMvc.perform(get("/api/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.preferredColors").isArray())
                .andExpect(jsonPath("$.data.preferredColors").isEmpty())
                .andExpect(jsonPath("$.data.preferredMaterials").isArray())
                .andExpect(jsonPath("$.data.preferredMaterials").isEmpty())
                .andExpect(jsonPath("$.data.styleTags").isArray())
                .andExpect(jsonPath("$.data.styleTags").isEmpty());
    }

    @Test
    void updatesAndReturnsCurrentUserPreferencesWithJsonArrayStorage() throws Exception {
        User targetUser = userRepository.save(User.createSeedUser("preferences-target-user"));
        User otherUser = userRepository.save(User.createSeedUser("preferences-other-user"));
        Map<String, Object> request = Map.of(
                "preferredColors", List.of("NAVY", "BLACK", "NAVY"),
                "preferredMaterials", List.of("COTTON", "WOOL", "COTTON"),
                "styleTags", List.of("MINIMAL", "CASUAL", "MINIMAL")
        );

        mockMvc.perform(put("/api/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.preferredColors[0]").value("NAVY"))
                .andExpect(jsonPath("$.data.preferredColors[1]").value("BLACK"))
                .andExpect(jsonPath("$.data.preferredColors[2]").doesNotExist())
                .andExpect(jsonPath("$.data.preferredMaterials[0]").value("COTTON"))
                .andExpect(jsonPath("$.data.preferredMaterials[1]").value("WOOL"))
                .andExpect(jsonPath("$.data.preferredMaterials[2]").doesNotExist())
                .andExpect(jsonPath("$.data.styleTags[0]").value("MINIMAL"))
                .andExpect(jsonPath("$.data.styleTags[1]").value("CASUAL"))
                .andExpect(jsonPath("$.data.styleTags[2]").doesNotExist());

        mockMvc.perform(get("/api/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredColors[0]").value("NAVY"))
                .andExpect(jsonPath("$.data.preferredColors[1]").value("BLACK"))
                .andExpect(jsonPath("$.data.preferredMaterials[0]").value("COTTON"))
                .andExpect(jsonPath("$.data.preferredMaterials[1]").value("WOOL"))
                .andExpect(jsonPath("$.data.styleTags[0]").value("MINIMAL"))
                .andExpect(jsonPath("$.data.styleTags[1]").value("CASUAL"));

        entityManager.flush();
        entityManager.clear();

        User saved = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(saved.getPreferredColorsJson()).isEqualTo("[\"NAVY\",\"BLACK\"]");
        assertThat(saved.getPreferredMaterialsJson()).isEqualTo("[\"COTTON\",\"WOOL\"]");
        assertThat(saved.getStyleTagsJson()).isEqualTo("[\"MINIMAL\",\"CASUAL\"]");

        User unchangedOtherUser = userRepository.findById(otherUser.getId()).orElseThrow();
        assertThat(unchangedOtherUser.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(unchangedOtherUser.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(unchangedOtherUser.getStyleTagsJson()).isEqualTo("[]");
    }

    @Test
    void rejectsInvalidEnumValueAsInvalidEnumValue() throws Exception {
        User user = userRepository.save(User.createSeedUser("preferences-invalid-enum-user"));
        String request = """
                {
                  "preferredColors": ["NOT_A_COLOR"],
                  "preferredMaterials": ["COTTON"],
                  "styleTags": ["MINIMAL"]
                }
                """;

        mockMvc.perform(put("/api/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void rejectsBlankOrTooLongStyleTagsAsInvalidRequest() throws Exception {
        User user = userRepository.save(User.createSeedUser("preferences-invalid-tags-user"));
        Map<String, Object> blankTagRequest = Map.of(
                "preferredColors", List.of("NAVY"),
                "preferredMaterials", List.of("COTTON"),
                "styleTags", List.of(" ")
        );
        Map<String, Object> tooLongTagRequest = Map.of(
                "preferredColors", List.of("NAVY"),
                "preferredMaterials", List.of("COTTON"),
                "styleTags", List.of("1234567890123456789012345678901")
        );

        mockMvc.perform(put("/api/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankTagRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("METHOD_ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(put("/api/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLongTagRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("METHOD_ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsUserNotFoundWithExistingErrorShape() throws Exception {
        String missingUserToken = "Bearer " + jwtTokenProvider.createAccessToken(
                new CurrentUserPrincipal(99999L, "missing-preferences-user@example.com", UserRole.USER));

        mockMvc.perform(get("/api/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, missingUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }
}
