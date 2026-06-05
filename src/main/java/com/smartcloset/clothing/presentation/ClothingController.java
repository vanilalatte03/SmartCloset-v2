package com.smartcloset.clothing.presentation;

import com.smartcloset.clothing.application.ClothingService;
import com.smartcloset.clothing.dto.ClothingArchiveResponse;
import com.smartcloset.clothing.dto.ClothingImageFileResponse;
import com.smartcloset.clothing.dto.ClothingRequest;
import com.smartcloset.clothing.dto.ClothingResponse;
import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/clothes")
public class ClothingController {

    private final ClothingService clothingService;

    public ClothingController(ClothingService clothingService) {
        this.clothingService = clothingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClothingResponse>> createClothing(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody ClothingRequest request
    ) {
        ClothingResponse response = clothingService.createClothing(
                principal.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ApiResponse<List<ClothingResponse>> getActiveClothes(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.getActiveClothes(principal.userId()));
    }

    @GetMapping("/archived")
    public ApiResponse<List<ClothingResponse>> getArchivedClothes(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.getArchivedClothes(principal.userId()));
    }

    @GetMapping("/{clothingId}")
    public ApiResponse<ClothingResponse> getClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.getClothing(principal.userId(), clothingId));
    }

    @PutMapping("/{clothingId}")
    public ApiResponse<ClothingResponse> updateClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody ClothingRequest request
    ) {
        return ApiResponse.of(clothingService.updateClothing(principal.userId(), clothingId, request));
    }

    @PatchMapping("/{clothingId}/archive")
    public ApiResponse<ClothingArchiveResponse> archiveClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.archiveClothing(principal.userId(), clothingId));
    }

    @PatchMapping("/{clothingId}/unarchive")
    public ApiResponse<ClothingArchiveResponse> unarchiveClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.unarchiveClothing(principal.userId(), clothingId));
    }

    @PutMapping(value = "/{clothingId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ClothingResponse> uploadClothingImage(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.of(clothingService.uploadClothingImage(principal.userId(), clothingId, image));
    }

    @GetMapping("/{clothingId}/image")
    public ResponseEntity<byte[]> getClothingImage(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        ClothingImageFileResponse response = clothingService.getClothingImage(principal.userId(), clothingId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.contentType()))
                .body(response.bytes());
    }

    @DeleteMapping("/{clothingId}/image")
    public ApiResponse<ClothingResponse> deleteClothingImage(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.deleteClothingImage(principal.userId(), clothingId));
    }
}
