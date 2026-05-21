package com.smartcloset.clothing.presentation;

import com.smartcloset.clothing.application.ClothingService;
import com.smartcloset.clothing.dto.ClothingRequest;
import com.smartcloset.clothing.dto.ClothingResponse;
import com.smartcloset.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clothes")
public class ClothingController {

    private final ClothingService clothingService;

    public ClothingController(ClothingService clothingService) {
        this.clothingService = clothingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClothingResponse>> createClothing(
            @RequestParam Long userId,
            @Valid @RequestBody ClothingRequest request
    ) {
        ClothingResponse response = clothingService.createClothing(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ApiResponse<List<ClothingResponse>> getActiveClothes(@RequestParam Long userId) {
        return ApiResponse.of(clothingService.getActiveClothes(userId));
    }
}
