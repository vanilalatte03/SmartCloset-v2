package com.smartcloset.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

class ProdProfileSafetyGuardTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProdProfileSafetyGuard.class);

    @Test
    void localProfileDoesNotCreateProdSafetyGuard() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "smartcloset.security.jwt.secret=change-me-local-development-only",
                        "spring.jpa.hibernate.ddl-auto=update"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ProdProfileSafetyGuard.class);
                });
    }

    @Test
    void prodProfileAcceptsNonLocalSecretAndSafeDdlAuto() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "smartcloset.security.jwt.secret=prod-secret-value",
                        "spring.jpa.hibernate.ddl-auto=validate"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ProdProfileSafetyGuard.class);
                });
    }

    @Test
    void prodProfileRejectsMissingJwtSecret() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "spring.jpa.hibernate.ddl-auto=validate"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("prod profile requires JWT_SECRET");
                });
    }

    @Test
    void prodProfileRejectsLocalJwtSecret() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "smartcloset.security.jwt.secret=change-me-local-development-only",
                        "spring.jpa.hibernate.ddl-auto=validate"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("prod profile must not use the local development JWT secret");
                });
    }

    @Test
    void prodProfileRejectsUnsafeDdlAuto() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "smartcloset.security.jwt.secret=prod-secret-value",
                        "spring.jpa.hibernate.ddl-auto=update"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("prod profile allows only spring.jpa.hibernate.ddl-auto");
                });
    }

    @Test
    void applicationProdDefaultsDisableDocsAndUseValidateDdlAuto() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(
                "application-prod",
                new ClassPathResource("application-prod.yml")
        );
        MutablePropertySources propertySources = new MutablePropertySources();
        sources.forEach(propertySources::addLast);
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);

        assertThat(resolver.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(resolver.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        assertThat(resolver.getProperty("spring.flyway.baseline-on-migrate", Boolean.class)).isFalse();
        assertThat(resolver.getProperty("smartcloset.security.jwt.secret")).isEmpty();
        assertThat(resolver.getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
        assertThat(resolver.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
    }
}
