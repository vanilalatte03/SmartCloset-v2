package com.smartcloset.clothing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClothingControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsClothingWithArchivedFalse() throws Exception {
        Map<String, Object> request = validRequest();

        mockMvc.perform(post("/api/clothes")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.name").value("그레이 후드"))
                .andExpect(jsonPath("$.data.category").value("TOP"))
                .andExpect(jsonPath("$.data.color").value("GRAY"))
                .andExpect(jsonPath("$.data.material").value("COTTON"))
                .andExpect(jsonPath("$.data.minTemperature").value(5))
                .andExpect(jsonPath("$.data.maxTemperature").value(18))
                .andExpect(jsonPath("$.data.rainSuitable").value(false))
                .andExpect(jsonPath("$.data.archived").value(false))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    void returnsOnlyActiveClothesForRequestedUserOrderedById() throws Exception {
        User targetUser = userRepository.save(User.createSeedUser("target-user"));
        User otherUser = userRepository.save(User.createSeedUser("other-user"));

        ClothingItem first = clothingItemRepository.save(ClothingItem.create(
                targetUser,
                "화이트 셔츠",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.COTTON,
                5,
                25,
                false
        ));
        ClothingItem archived = ClothingItem.create(
                targetUser,
                "보관 니트",
                ClothingCategory.TOP,
                ClothingColor.GRAY,
                ClothingMaterial.KNIT,
                0,
                16,
                false
        );
        archived.archive();
        clothingItemRepository.save(archived);
        ClothingItem second = clothingItemRepository.save(ClothingItem.create(
                targetUser,
                "블랙 팬츠",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                22,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                otherUser,
                "다른 사용자 코트",
                ClothingCategory.OUTER,
                ClothingColor.NAVY,
                ClothingMaterial.WOOL,
                -10,
                12,
                false
        ));
        clothingItemRepository.flush();

        MvcResult result = mockMvc.perform(get("/api/clothes")
                        .param("userId", targetUser.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data).hasSize(2);
        assertThat(data.get(0).get("id").asLong()).isEqualTo(first.getId());
        assertThat(data.get(1).get("id").asLong()).isEqualTo(second.getId());
        for (JsonNode item : data) {
            assertThat(item.get("userId").asLong()).isEqualTo(targetUser.getId());
            assertThat(item.get("archived").asBoolean()).isFalse();
        }
    }

    @Test
    void returnsInvalidRequestWhenRequestValidationFails() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("name", " ");
        request.put("minTemperature", 20);
        request.put("maxTemperature", 10);

        mockMvc.perform(post("/api/clothes")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsInvalidRequestWhenRequiredBooleanIsMissing() throws Exception {
        Map<String, Object> request = validRequest();
        request.remove("rainSuitable");

        mockMvc.perform(post("/api/clothes")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("rainSuitable"));
    }

    @Test
    void returnsUserNotFoundForUnknownUser() throws Exception {
        mockMvc.perform(get("/api/clothes")
                        .param("userId", "99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.details").isArray());
    }

    private Map<String, Object> validRequest() {
        return new java.util.LinkedHashMap<>(Map.of(
                "name", "그레이 후드",
                "category", "TOP",
                "color", "GRAY",
                "material", "COTTON",
                "minTemperature", 5,
                "maxTemperature", 18,
                "rainSuitable", false
        ));
    }
}
