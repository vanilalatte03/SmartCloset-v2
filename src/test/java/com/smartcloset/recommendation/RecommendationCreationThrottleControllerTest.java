package com.smartcloset.recommendation;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=",
        "WEATHER_FALLBACK_ENABLED=true",
        "smartcloset.recommendation.creation-throttle.max-requests=1",
        "smartcloset.recommendation.creation-throttle.window=10m"
})
@Transactional
class RecommendationCreationThrottleControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void repeatedRecommendationCreationForSameUserIsRateLimited() throws Exception {
        User user = createUserWithP0Closet("recommendation-throttle-user");
        User otherUser = createUserWithP0Closet("recommendation-throttle-other-user");

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_CREATION_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser)))
                .andExpect(status().isCreated());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    private User createUserWithP0Closet(String name) {
        User user = userRepository.save(User.createSeedUser(name));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "아이보리 니트",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.KNIT,
                0,
                16,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "블랙 데님",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                22,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "네이비 코트",
                ClothingCategory.OUTER,
                ClothingColor.NAVY,
                ClothingMaterial.WOOL,
                -10,
                12,
                false
        ));
        clothingItemRepository.flush();
        return user;
    }
}
