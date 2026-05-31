package com.smartcloset.user.presentation;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.user.application.AccountDeletionService;
import com.smartcloset.user.application.CurrentUserService;
import com.smartcloset.user.dto.AccountDeletionRequest;
import com.smartcloset.user.dto.AccountDeletionResponse;
import com.smartcloset.user.dto.CurrentUserResponse;
import com.smartcloset.user.dto.UpdateCurrentUserRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class CurrentUserController {

    private final CurrentUserService currentUserService;
    private final AccountDeletionService accountDeletionService;

    public CurrentUserController(CurrentUserService currentUserService, AccountDeletionService accountDeletionService) {
        this.currentUserService = currentUserService;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping
    public ApiResponse<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.of(currentUserService.getCurrentUser(principal.userId()));
    }

    @PatchMapping
    public ApiResponse<CurrentUserResponse> updateCurrentUser(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UpdateCurrentUserRequest request
    ) {
        return ApiResponse.of(currentUserService.updateCurrentUser(principal.userId(), request));
    }

    @DeleteMapping
    public ApiResponse<AccountDeletionResponse> deleteCurrentUser(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AccountDeletionRequest request
    ) {
        return ApiResponse.of(accountDeletionService.deleteAccount(principal.userId(), request));
    }
}
