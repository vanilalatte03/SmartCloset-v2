package com.smartcloset.common.config;

import java.util.Locale;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * prod profile이 local 편의 기본값으로 기동되는 것을 context 초기화 초기에 막는다.
 */
@Component
@Profile("prod")
public class ProdProfileSafetyGuard implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    static final String LOCAL_JWT_SECRET = "change-me-local-development-only";
    private static final Set<String> ALLOWED_DDL_AUTO_VALUES = Set.of("none", "validate");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        validateJwtSecret();
        validateDdlAuto();
        validateSecureCookies();
    }

    private void validateJwtSecret() {
        String secret = property("smartcloset.security.jwt.secret");
        if (secret.isBlank()) {
            throw new IllegalStateException("prod profile requires JWT_SECRET");
        }
        if (LOCAL_JWT_SECRET.equals(secret)) {
            throw new IllegalStateException("prod profile must not use the local development JWT secret");
        }
    }

    private void validateDdlAuto() {
        String ddlAuto = property("spring.jpa.hibernate.ddl-auto").toLowerCase(Locale.ROOT);
        if (!ALLOWED_DDL_AUTO_VALUES.contains(ddlAuto)) {
            throw new IllegalStateException(
                    "prod profile allows only spring.jpa.hibernate.ddl-auto=validate or none"
            );
        }
    }

    private void validateSecureCookies() {
        validateTrue(
                "smartcloset.security.refresh-token.cookie.secure",
                "prod profile requires refresh cookie Secure=true"
        );
        validateTrue(
                "smartcloset.security.oauth2.state-cookie.secure",
                "prod profile requires OAuth state cookie Secure=true"
        );
    }

    private void validateTrue(String name, String message) {
        if (!Boolean.parseBoolean(property(name))) {
            throw new IllegalStateException(message);
        }
    }

    private String property(String name) {
        if (environment == null) {
            return "";
        }
        return environment.getProperty(name, "").trim();
    }
}
