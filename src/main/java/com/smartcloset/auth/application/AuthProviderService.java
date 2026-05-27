package com.smartcloset.auth.application;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthProviderService {

    private final SocialAccountRepository socialAccountRepository;

    public AuthProviderService(SocialAccountRepository socialAccountRepository) {
        this.socialAccountRepository = socialAccountRepository;
    }

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

    public List<String> providersFor(User user, OAuthProvider linkedProvider) {
        List<String> providers = new ArrayList<>();
        if (user.isPasswordLoginEnabled()) {
            providers.add("PASSWORD");
        }
        providers.add(linkedProvider.name());
        return providers.stream().distinct().toList();
    }
}
