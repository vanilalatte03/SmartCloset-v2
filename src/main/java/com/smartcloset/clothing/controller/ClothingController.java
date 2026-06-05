package com.smartcloset.clothing.controller;

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

/**
 * 인증 사용자의 옷장 CRUD, 보관함, 단일 이미지 관리를 노출하는 HTTP adapter다.
 *
 * <p>Controller는 principal에서 current user id만 추출하고, 소유자 검증과 저장소 정책은
 * {@link ClothingService}에 위임한다.</p>
 */
@RestController
@RequestMapping("/api/clothes")
public class ClothingController {

    private final ClothingService clothingService;

    public ClothingController(ClothingService clothingService) {
        this.clothingService = clothingService;
    }

    /**
     * 현재 인증 사용자의 새 옷을 등록하고 생성된 옷 DTO를 반환한다.
     */
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

    /**
     * 추천 후보로 쓰이는 현재 사용자의 활성 옷 목록을 반환한다.
     */
    @GetMapping
    public ApiResponse<List<ClothingResponse>> getActiveClothes(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.getActiveClothes(principal.userId()));
    }

    /**
     * 보관 상태라 추천 후보에서 제외된 현재 사용자의 옷 목록을 반환한다.
     */
    @GetMapping("/archived")
    public ApiResponse<List<ClothingResponse>> getArchivedClothes(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.getArchivedClothes(principal.userId()));
    }

    /**
     * 현재 사용자가 소유한 단일 옷 상세를 조회한다.
     */
    @GetMapping("/{clothingId}")
    public ApiResponse<ClothingResponse> getClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.getClothing(principal.userId(), clothingId));
    }

    /**
     * 현재 사용자가 소유한 옷의 기본 정보와 style tag를 전체 교체한다.
     */
    @PutMapping("/{clothingId}")
    public ApiResponse<ClothingResponse> updateClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody ClothingRequest request
    ) {
        return ApiResponse.of(clothingService.updateClothing(principal.userId(), clothingId, request));
    }

    /**
     * 옷을 보관 상태로 바꿔 추천 후보와 기본 옷장 목록에서 제외한다.
     */
    @PatchMapping("/{clothingId}/archive")
    public ApiResponse<ClothingArchiveResponse> archiveClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.archiveClothing(principal.userId(), clothingId));
    }

    /**
     * 보관된 옷을 활성 상태로 되돌려 추천 후보에 다시 포함될 수 있게 한다.
     */
    @PatchMapping("/{clothingId}/unarchive")
    public ApiResponse<ClothingArchiveResponse> unarchiveClothing(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.unarchiveClothing(principal.userId(), clothingId));
    }

    /**
     * 현재 사용자가 소유한 옷의 단일 이미지를 업로드하거나 기존 이미지를 교체한다.
     */
    @PutMapping(value = "/{clothingId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ClothingResponse> uploadClothingImage(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.of(clothingService.uploadClothingImage(principal.userId(), clothingId, image));
    }

    /**
     * 보호 이미지라서 JSON wrapper 대신 bytes를 직접 반환한다. 인증은 JWT filter와 소유자 조회가 맡는다.
     */
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

    /**
     * 이미지가 없는 경우에도 성공을 유지하며 옷의 이미지 메타데이터를 비운다.
     */
    @DeleteMapping("/{clothingId}/image")
    public ApiResponse<ClothingResponse> deleteClothingImage(
            @PathVariable Long clothingId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(clothingService.deleteClothingImage(principal.userId(), clothingId));
    }
}
