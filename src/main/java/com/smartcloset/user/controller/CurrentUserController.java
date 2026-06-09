package com.smartcloset.user.controller;

import com.smartcloset.auth.infrastructure.RefreshTokenCookieWriter;
import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.user.application.AccountDeletionService;
import com.smartcloset.user.application.CurrentUserService;
import com.smartcloset.user.dto.AccountDeletionRequest;
import com.smartcloset.user.dto.AccountDeletionResponse;
import com.smartcloset.user.dto.CurrentUserResponse;
import com.smartcloset.user.dto.UpdateCurrentUserRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 인증 사용자의 프로필 조회/수정과 계정 삭제 endpoint를 제공한다.
 *
 * <p>응답 DTO는 현재 사용자 전용 계약을 따르므로 userId를 외부에 노출하지 않는다.</p>
 */
@RestController
@RequestMapping("/api/users/me")
public class CurrentUserController {

    private final CurrentUserService currentUserService;
    private final AccountDeletionService accountDeletionService;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    public CurrentUserController(
            CurrentUserService currentUserService,
            AccountDeletionService accountDeletionService,
            RefreshTokenCookieWriter refreshTokenCookieWriter
    ) {
        this.currentUserService = currentUserService;
        this.accountDeletionService = accountDeletionService;
        this.refreshTokenCookieWriter = refreshTokenCookieWriter;
    }

    /**
     * 현재 인증 사용자의 프로필과 연결된 로그인 provider 목록을 반환한다.
     */
    @GetMapping
    public ApiResponse<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.of(currentUserService.getCurrentUser(principal.userId()));
    }

    /**
     * 현재 인증 사용자의 표시 이름을 수정하고 갱신된 프로필 DTO를 반환한다.
     */
    @PatchMapping
    public ApiResponse<CurrentUserResponse> updateCurrentUser(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UpdateCurrentUserRequest request
    ) {
        return ApiResponse.of(currentUserService.updateCurrentUser(principal.userId(), request));
    }

    /**
     * 확인 문구와 필요한 경우 비밀번호를 검증한 뒤 현재 계정을 hard delete한다.
     */
    @DeleteMapping
    public ApiResponse<AccountDeletionResponse> deleteCurrentUser(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AccountDeletionRequest request,
            HttpServletResponse response
    ) {
        AccountDeletionResponse deletionResponse = accountDeletionService.deleteAccount(principal.userId(), request);
        refreshTokenCookieWriter.expire(response);
        return ApiResponse.of(deletionResponse);
    }
}
