package com.smartcloset.auth.application;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 계정에 연결된 로그인 provider 목록을 response-friendly 문자열 목록으로 조합한다.
 *
 * <p>Password login 가능 여부와 social account 연결 상태를 함께 반영한다.</p>
 */
@Service
public class AuthProviderService {

    private final SocialAccountRepository socialAccountRepository;

    public AuthProviderService(SocialAccountRepository socialAccountRepository) {
        this.socialAccountRepository = socialAccountRepository;
    }

    /**
     * DB에 저장된 social account와 password login 가능 여부를 합쳐 현재 provider 목록을 만든다.
     */
    @Transactional(readOnly = true)
    public List<String> providersFor(User user) {
        List<String> providers = new ArrayList<>();
        if (user.isPasswordLoginEnabled()) {
            providers.add("PASSWORD");
        }
        socialAccountRepository.findByUserId(user.getId()).stream()
                .map(account -> account.getProvider().name())
                .distinct()
                .sorted()
                .forEach(providers::add);
        return providers;
    }

    /**
     * 방금 연결된 provider까지 포함해 아직 flush 전인 callback 응답 provider 목록을 만든다.
     */
    public List<String> providersFor(User user, OAuthProvider linkedProvider) {
        List<String> providers = new ArrayList<>();
        if (user.isPasswordLoginEnabled()) {
            providers.add("PASSWORD");
        }
        providers.add(linkedProvider.name());
        return providers.stream().distinct().toList();
    }
}
